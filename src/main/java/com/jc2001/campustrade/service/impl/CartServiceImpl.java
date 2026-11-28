package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.dto.request.AddCartItemRequest;
import com.jc2001.campustrade.dto.request.UpdateCartItemRequest;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.entity.UserEntity;
import com.jc2001.campustrade.repository.UserRepository;
import com.jc2001.campustrade.security.AuthenticatedUser;
import com.jc2001.campustrade.service.CartService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车实现（切片 3A，FR-301 / FR-302）。
 *
 * <p>数量固定为 1（A-16）；同一商品不重复加入（R-04，唯一约束兜底）；
 * 失效条目不报错，而是以 {@code purchasable=false + unavailableReason} 返回，供前端灰显。
 */
@Service
public class CartServiceImpl implements CartService {

    private static final String SELECT =
            "SELECT ci.id cart_item_id, ci.quantity, ci.created_at added_at, "
            + "i.id, i.title, i.estimated_price, i.status, i.seller_id, i.category_id, i.condition_id, "
            + "i.trade_zone_id, i.swap_preference, i.published_at, "
            + "c.name category_name, ic.name condition_name, z.name zone_name, "
            + "u.nickname seller_nickname, u.avatar_url seller_avatar, u.created_at seller_created, "
            + "(SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY sort_order,id LIMIT 1) cover "
            + "FROM cart_items ci "
            + "JOIN items i ON i.id=ci.item_id AND i.deleted_at IS NULL "
            + "JOIN categories c ON c.id=i.category_id "
            + "JOIN item_conditions ic ON ic.id=i.condition_id "
            + "JOIN trade_zones z ON z.id=i.trade_zone_id "
            + "JOIN users u ON u.id=i.seller_id ";

    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public CartServiceImpl(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Override public CartView getCart() {
        Long uid = currentUserId();
        List<Map<String, Object>> rows = jdbc.queryForList(SELECT + "WHERE ci.user_id=? ORDER BY ci.created_at DESC, ci.id DESC", uid);
        Map<Long, CartGroup> groups = new LinkedHashMap<>();
        int totalItems = 0;
        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            CartItem item = toCartItem(row);
            totalItems++;
            if (Boolean.TRUE.equals(item.getPurchasable()) && item.getItem() != null && item.getItem().getEstimatedPrice() != null) {
                totalAmount = totalAmount.add(new java.math.BigDecimal(item.getItem().getEstimatedPrice().getValue()));
            }
            Long sellerId = ((Number) row.get("seller_id")).longValue();
            CartGroup group = groups.get(sellerId);
            if (group == null) {
                group = new CartGroup();
                UserProfile seller = new UserProfile();
                seller.setId(sellerId);
                seller.setNickname((String) row.get("seller_nickname"));
                seller.setAvatarUrl((String) row.get("seller_avatar"));
                Timestamp sc = (Timestamp) row.get("seller_created");
                if (sc != null) seller.setCreatedAt(sc.toInstant());
                group.setSeller(seller);
                group.setItems(new ArrayList<>());
                group.setGroupTotal(DecimalString.of(java.math.BigDecimal.ZERO));
                group.setPurchasableCount(0);
                groups.put(sellerId, group);
            }
            group.getItems().add(item);
            if (Boolean.TRUE.equals(item.getPurchasable())) {
                group.setPurchasableCount(group.getPurchasableCount() + 1);
                if (item.getItem().getEstimatedPrice() != null) {
                    group.setGroupTotal(DecimalString.of(new java.math.BigDecimal(group.getGroupTotal().getValue())
                            .add(new java.math.BigDecimal(item.getItem().getEstimatedPrice().getValue()))));
                }
            }
        }
        CartView view = new CartView();
        view.setGroups(new ArrayList<>(groups.values()));
        view.setTotalItems(totalItems);
        view.setTotalAmount(DecimalString.of(totalAmount));
        view.setPriceDisclaimer("用户自填估价，仅供参考，非平台定价");
        return view;
    }

    @Override @Transactional public CartItem addCartItem(String idempotencyKey, AddCartItemRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        if (body == null || body.getItemId() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        Long itemId = body.getItemId();

        Map<String, Object> item;
        try { item = jdbc.queryForMap("SELECT id, seller_id, status::text status FROM items WHERE id=? AND deleted_at IS NULL", itemId); }
        catch (org.springframework.dao.EmptyResultDataAccessException ex) { throw new DomainException(ErrorCode.ITEM_NOT_FOUND); }

        Long sellerId = ((Number) item.get("seller_id")).longValue();
        if (sellerId.equals(uid)) throw new DomainException(ErrorCode.CART_OWN_ITEM_NOT_ALLOWED);
        String status = (String) item.get("status");
        if (!"IN_SALE".equals(status)) {
            if ("RESERVED".equals(status)) throw new DomainException(ErrorCode.ITEM_ALREADY_RESERVED);
            if ("SWAPPING".equals(status)) throw new DomainException(ErrorCode.ITEM_RESERVED_BY_SWAP);
            throw new DomainException(ErrorCode.ITEM_NOT_PURCHASABLE);
        }
        // R-04 / uq_cart_items_user_item：重复加入幂等，不产生第二行
        jdbc.update("INSERT INTO cart_items(user_id, item_id, quantity, created_by) VALUES (?,?,1,?) ON CONFLICT (user_id, item_id) DO NOTHING",
                uid, itemId, uid);
        return oneByUserAndItem(uid, itemId);
    }

    @Override @Transactional public CartItem updateCartItem(Long cartItemId, UpdateCartItemRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> row = ownedCartRow(cartItemId, uid);
        return oneByCartItemId(((Number) row.get("cart_item_id")).longValue(), uid);
    }

    @Override @Transactional public Void removeCartItem(Long cartItemId) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> row = null;
        try { row = jdbc.queryForMap("SELECT id, user_id FROM cart_items WHERE id=?", cartItemId); }
        catch (org.springframework.dao.EmptyResultDataAccessException ex) { return null; } // 幂等：不存在视为成功
        if (!Long.valueOf(((Number) row.get("user_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);
        jdbc.update("DELETE FROM cart_items WHERE id=? AND user_id=?", cartItemId, uid);
        return null;
    }

    // ------------------------------------------------------------------

    private Map<String, Object> ownedCartRow(Long cartItemId, Long uid) {
        Map<String, Object> row;
        try { row = jdbc.queryForMap("SELECT ci.id cart_item_id, ci.user_id FROM cart_items ci WHERE ci.id=?", cartItemId); }
        catch (org.springframework.dao.EmptyResultDataAccessException ex) { throw new DomainException(ErrorCode.CART_ITEM_NOT_FOUND); }
        if (!Long.valueOf(((Number) row.get("user_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);
        return row;
    }

    private CartItem oneByUserAndItem(Long uid, Long itemId) {
        List<Map<String, Object>> rows = jdbc.queryForList(SELECT + "WHERE ci.user_id=? AND ci.item_id=?", uid, itemId);
        if (rows.isEmpty()) throw new DomainException(ErrorCode.CART_ITEM_NOT_FOUND);
        return toCartItem(rows.get(0));
    }

    private CartItem oneByCartItemId(Long cartItemId, Long uid) {
        List<Map<String, Object>> rows = jdbc.queryForList(SELECT + "WHERE ci.id=? AND ci.user_id=?", cartItemId, uid);
        if (rows.isEmpty()) throw new DomainException(ErrorCode.CART_ITEM_NOT_FOUND);
        return toCartItem(rows.get(0));
    }

    private CartItem toCartItem(Map<String, Object> row) {
        CartItem ci = new CartItem();
        ci.setId(((Number) row.get("cart_item_id")).longValue());
        ci.setItemId(((Number) row.get("id")).longValue());
        ci.setQuantity(((Number) row.get("quantity")).intValue());
        Timestamp added = (Timestamp) row.get("added_at");
        if (added != null) ci.setAddedAt(added.toInstant());

        String status = (String) row.get("status");
        boolean purchasable = "IN_SALE".equals(status);
        ci.setPurchasable(purchasable);
        ci.setUnavailableReason(purchasable ? null : unavailableReason(status));

        ItemSummary s = new ItemSummary();
        s.setId(((Number) row.get("id")).longValue());
        s.setTitle((String) row.get("title"));
        s.setCoverImageUrl((String) row.get("cover"));
        s.setEstimatedPrice(DecimalString.of((java.math.BigDecimal) row.get("estimated_price")));
        s.setCurrency("CNY");
        s.setStatus(ItemStatus.valueOf(status));
        s.setSwapPreference(SwapPreference.valueOf((String) row.get("swap_preference")));
        s.setCategoryId(((Number) row.get("category_id")).longValue());
        s.setCategoryName((String) row.get("category_name"));
        s.setConditionId(((Number) row.get("condition_id")).longValue());
        s.setConditionName((String) row.get("condition_name"));
        s.setTradeZoneId(((Number) row.get("trade_zone_id")).longValue());
        s.setTradeZoneName((String) row.get("zone_name"));
        s.setSellerId(((Number) row.get("seller_id")).longValue());
        s.setSellerNickname((String) row.get("seller_nickname"));
        Timestamp published = (Timestamp) row.get("published_at");
        if (published != null) s.setPublishedAt(published.toInstant());
        ci.setItem(s);
        return ci;
    }

    private String unavailableReason(String status) {
        switch (status) {
            case "RESERVED": return "商品已被其他订单预订";
            case "SWAPPING": return "商品正在交换中";
            case "SOLD": return "商品已售出";
            case "OFF_SHELF": return "商品已下架";
            case "BANNED": return "商品已违规下架";
            case "DRAFT": return "商品未上架";
            default: return "商品当前不可购买";
        }
    }

    private void ensureNotRestricted(Long uid) {
        UserEntity u = users.findById(uid).orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND));
        if ("RESTRICTED".equals(u.getStatus())) throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser)) throw new DomainException(ErrorCode.UNAUTHORIZED);
        return ((AuthenticatedUser) auth.getPrincipal()).getUserId();
    }
}
