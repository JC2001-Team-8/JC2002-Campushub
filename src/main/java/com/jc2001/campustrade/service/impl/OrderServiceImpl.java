package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.*;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.security.AuthenticatedUser;
import com.jc2001.campustrade.service.IdempotencyService;
import com.jc2001.campustrade.service.ItemService;
import com.jc2001.campustrade.service.OrderService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单与付款实现（切片 3A，FR-303 ~ FR-311）。
 *
 * <p>要点：
 * <ul>
 *   <li>R-16 占用发生在下单时，不只在付款时；占用与 {@code orders} 写入同事务。</li>
 *   <li>R-05 / C-02 一单一卖家：按卖家自动拆单。</li>
 *   <li>R-07 付款超时 30 分钟；付款与超时取消用条件更新互斥。</li>
 *   <li>R-13 订单明细做标题/价格快照。</li>
 *   <li>R-18 状态推进全部条件更新，禁止「先查后改」。</li>
 * </ul>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Duration PAYMENT_WINDOW = Duration.ofMinutes(30);
    private static final Duration REVIEW_WINDOW = Duration.ofDays(7);
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private static final String ORDER_SELECT =
            "SELECT o.*, z.code zone_code, z.name zone_name, pp.code pickup_code, pp.name pickup_name, "
            + "pp.point_type pickup_type, bu.nickname buyer_nickname, bu.avatar_url buyer_avatar, bu.created_at buyer_created, "
            + "su.nickname seller_nickname, su.avatar_url seller_avatar, su.created_at seller_created, "
            + "(SELECT count(*) FROM order_items oi WHERE oi.order_id=o.id) item_count, "
            + "(SELECT im.image_url FROM order_items oi JOIN item_images im ON im.item_id=oi.item_id "
            + " WHERE oi.order_id=o.id ORDER BY oi.id, im.sort_order LIMIT 1) cover "
            + "FROM orders o "
            + "JOIN trade_zones z ON z.id=o.trade_zone_id "
            + "LEFT JOIN pickup_points pp ON pp.id=o.pickup_point_id "
            + "JOIN users bu ON bu.id=o.buyer_id "
            + "JOIN users su ON su.id=o.seller_id ";

    private final JdbcTemplate jdbc;
    private final ItemService items;
    private final IdempotencyService idempotency;

    public OrderServiceImpl(JdbcTemplate jdbc, ItemService items, IdempotencyService idempotency) {
        this.jdbc = jdbc;
        this.items = items;
        this.idempotency = idempotency;
    }

    // =================================================================
    // 提交订单
    // =================================================================

    @Override @Transactional public SubmitOrderResponse submitOrder(String idempotencyKey, SubmitOrderRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "submitOrder", body, SubmitOrderResponse.class,
                () -> submitOnce(uid, body));
    }

    private SubmitOrderResponse submitOnce(Long uid, SubmitOrderRequest body) {
        ensureNotRestricted(uid);
        if (body == null || body.getCartItemIds() == null || body.getCartItemIds().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR);
        }
        if (body.getTradeZoneId() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        ensureZoneSelected(uid, body.getTradeZoneId());

        List<Map<String, Object>> rows = loadCartRows(uid, body.getCartItemIds());
        if (rows.size() != new HashSet<>(body.getCartItemIds()).size()) {
            throw new DomainException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        for (Map<String, Object> r : rows) {
            String st = (String) r.get("status");
            if (!"IN_SALE".equals(st)) {
                if ("RESERVED".equals(st)) throw new DomainException(ErrorCode.ITEM_ALREADY_RESERVED);
                if ("SWAPPING".equals(st)) throw new DomainException(ErrorCode.ITEM_RESERVED_BY_SWAP);
                throw new DomainException(ErrorCode.ITEM_NOT_PURCHASABLE);
            }
        }

        // C-02 / R-05：按卖家拆单
        Map<Long, List<Map<String, Object>>> bySeller = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            bySeller.computeIfAbsent(((Number) r.get("seller_id")).longValue(), k -> new ArrayList<>()).add(r);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(PAYMENT_WINDOW);
        List<Long> created = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> e : bySeller.entrySet()) {
            Long sellerId = e.getKey();
            List<Map<String, Object>> group = e.getValue();
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> r : group) total = total.add((BigDecimal) r.get("estimated_price"));

            Long orderId = jdbc.queryForObject(
                    "INSERT INTO orders(order_no, buyer_id, seller_id, trade_zone_id, status, estimated_total, expires_at, created_by) "
                    + "VALUES (?,?,?,?,'PENDING_PAYMENT',?::numeric,?,?) RETURNING id",
                    Long.class, nextOrderNo(), uid, sellerId, body.getTradeZoneId(), total.toPlainString(),
                    Timestamp.from(expiresAt), uid);

            for (Map<String, Object> r : group) {
                Long itemId = ((Number) r.get("item_id")).longValue();
                // R-16：先占用（条件更新，冲突即回滚整单），再写明细
                items.occupyForOrder(itemId, orderId, uid);
                jdbc.update("INSERT INTO order_items(order_id, item_id, title_snapshot, price_snapshot, condition_snapshot, created_by) "
                        + "VALUES (?,?,?,?::numeric,?,?)", orderId, itemId, r.get("title"),
                        ((BigDecimal) r.get("estimated_price")).toPlainString(), r.get("condition_name"), uid);
            }
            created.add(orderId);
        }
        // 清空已下单条目
        for (Map<String, Object> r : rows) {
            jdbc.update("DELETE FROM cart_items WHERE user_id=? AND item_id=?", uid, ((Number) r.get("item_id")).longValue());
        }

        SubmitOrderResponse resp = new SubmitOrderResponse();
        List<OrderDetail> details = new ArrayList<>();
        for (Long id : created) details.add(detail(id, uid));
        resp.setOrders(details);
        resp.setOrderCount(details.size());
        resp.setSplit(details.size() > 1);
        return resp;
    }

    private List<Map<String, Object>> loadCartRows(Long uid, List<Long> cartItemIds) {
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < cartItemIds.size(); i++) { if (i > 0) in.append(','); in.append('?'); }
        Object[] args = new Object[cartItemIds.size() + 1];
        args[0] = uid;
        for (int i = 0; i < cartItemIds.size(); i++) args[i + 1] = cartItemIds.get(i);
        return jdbc.queryForList(
                "SELECT ci.id cart_item_id, ci.item_id, i.seller_id, i.title, i.estimated_price, i.status::text status, "
                + "ic.name condition_name "
                + "FROM cart_items ci JOIN items i ON i.id=ci.item_id AND i.deleted_at IS NULL "
                + "JOIN item_conditions ic ON ic.id=i.condition_id "
                + "WHERE ci.user_id=? AND ci.id IN (" + in + ")", args);
    }

    private String nextOrderNo() {
        String ts = ORDER_NO_FMT.format(Instant.now());
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "JC" + ts + rand;
    }

    // =================================================================
    // 列表 / 详情
    // =================================================================

    @Override public PageResult<OrderSummary> listOrders(Integer page, Integer size, List<String> sort, String role, List<OrderStatus> status) {
        Long uid = currentUserId();
        int p = page == null ? 0 : page, n = size == null ? 20 : size;
        if (p < 0 || n < 1 || n > 100) throw new DomainException(ErrorCode.VALIDATION_ERROR);

        String order = "o.created_at DESC,o.id DESC";
        if (sort != null && !sort.isEmpty()) {
            List<String> terms = new ArrayList<>();
            for (String s : sort) {
                String[] a = s.split(",");
                if (!"created_at".equals(a[0])) throw new DomainException(ErrorCode.COMMON_INVALID_SORT_FIELD);
                String dir = a.length > 1 ? a[1].toUpperCase(Locale.ROOT) : "ASC";
                if (!"ASC".equals(dir) && !"DESC".equals(dir)) throw new DomainException(ErrorCode.VALIDATION_ERROR);
                terms.add("o.created_at " + dir);
            }
            order = String.join(",", terms);
        }

        StringBuilder where = new StringBuilder(" WHERE (o.buyer_id=? OR o.seller_id=?)");
        List<Object> args = new ArrayList<>(Arrays.asList(uid, uid));
        if (role != null && !role.trim().isEmpty()) {
            String r = role.trim().toUpperCase(Locale.ROOT);
            if ("BUYER".equals(r)) where.append(" AND o.buyer_id=?");
            else if ("SELLER".equals(r)) where.append(" AND o.seller_id=?");
            else throw new DomainException(ErrorCode.VALIDATION_ERROR);
            args.add(uid);
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND o.status::text IN (");
            for (int i = 0; i < status.size(); i++) { if (i > 0) where.append(','); where.append('?'); args.add(status.get(i).name()); }
            where.append(')');
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM orders o" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(n); pageArgs.add(p * n);
        List<OrderSummary> rows = jdbc.query(ORDER_SELECT + where + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                (rs, i) -> summaryFrom(rs, uid), pageArgs.toArray());
        return PageResult.of(p, n, total == null ? 0 : total, rows);
    }

    @Override public OrderDetail getOrderDetail(Long orderId) {
        Long uid = currentUserId();
        return detail(orderId, uid, true);
    }

    // =================================================================
    // 付款 / 取消
    // =================================================================

    @Override @Transactional public PaymentResult mockPayOrder(Long orderId, String idempotencyKey, MockPayRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "mockPayOrder", body, PaymentResult.class,
                () -> payOnce(uid, orderId));
    }

    private PaymentResult payOnce(Long uid, Long orderId) {
        ensureNotRestricted(uid);
        Map<String, Object> o = orderRow(orderId);
        if (!Long.valueOf(((Number) o.get("buyer_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);

        String status = (String) o.get("status");
        if ("PAID".equals(status) || "DELIVERED".equals(status) || "COMPLETED".equals(status)) {
            return paymentResult(o); // IDEM-A：重复付款返回首次成功结果
        }
        if ("CANCELLED".equals(status)) throw new DomainException(ErrorCode.ORDER_ALREADY_CANCELLED);
        if ("TIMEOUT_CANCELLED".equals(status)) throw new DomainException(ErrorCode.ORDER_TIMEOUT_RELEASED);
        if (!"PENDING_PAYMENT".equals(status)) throw new DomainException(ErrorCode.ORDER_STATUS_NOT_PAYABLE);

        Timestamp expires = (Timestamp) o.get("expires_at");
        if (expires == null || !expires.toInstant().isAfter(Instant.now())) {
            timeoutCancel(orderId, uid);
            throw new DomainException(ErrorCode.ORDER_TIMEOUT_RELEASED);
        }

        int n = jdbc.update("UPDATE orders SET status='PAID', paid_at=now(), version=version+1, updated_by=? "
                + "WHERE id=? AND status='PENDING_PAYMENT' AND now() < expires_at", uid, orderId);
        if (n != 1) {
            Map<String, Object> again = orderRow(orderId);
            String st = (String) again.get("status");
            if ("TIMEOUT_CANCELLED".equals(st)) throw new DomainException(ErrorCode.ORDER_TIMEOUT_RELEASED);
            if ("CANCELLED".equals(st)) throw new DomainException(ErrorCode.ORDER_ALREADY_CANCELLED);
            if ("PAID".equals(st)) return paymentResult(again);
            throw new DomainException(ErrorCode.ORDER_STATUS_NOT_PAYABLE);
        }
        BigDecimal amount = (BigDecimal) o.get("estimated_total");
        jdbc.update("INSERT INTO payments(order_id, payer_id, channel, status, amount, created_by) "
                + "VALUES (?,?,'MOCK','SUCCEEDED',?::numeric,?)", orderId, uid, amount.toPlainString(), uid);
        return paymentResult(orderRow(orderId));
    }

    @Override @Transactional public OrderDetail cancelOrder(Long orderId, String idempotencyKey, CancelOrderRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> o = orderRow(orderId);
        if (!Long.valueOf(((Number) o.get("buyer_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);

        String status = (String) o.get("status");
        if ("CANCELLED".equals(status)) return detail(orderId, uid, true); // IDEM-A
        if ("TIMEOUT_CANCELLED".equals(status)) throw new DomainException(ErrorCode.ORDER_ALREADY_CANCELLED);
        if (!"PENDING_PAYMENT".equals(status)) throw new DomainException(ErrorCode.ORDER_CANNOT_CANCEL_AFTER_PAID);

        String note = body == null ? null : body.getReason();
        int n = jdbc.update("UPDATE orders SET status='CANCELLED', cancelled_at=now(), cancel_reason='BUYER_CANCELLED', "
                + "cancel_note=?, version=version+1, updated_by=? WHERE id=? AND status='PENDING_PAYMENT'",
                note, uid, orderId);
        if (n != 1) {
            String st = (String) orderRow(orderId).get("status");
            if ("CANCELLED".equals(st)) return detail(orderId, uid, true);
            throw new DomainException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        for (Long itemId : orderItemIds(orderId)) items.releaseOrderOccupation(itemId, orderId, uid, "BUYER_CANCELLED");
        return detail(orderId, uid, true);
    }

    /** 超时取消（幂等，供定时任务与付款前置校验共用）。 */
    @Transactional public boolean timeoutCancel(Long orderId, Long actorId) {
        int n = jdbc.update("UPDATE orders SET status='TIMEOUT_CANCELLED', cancelled_at=now(), cancel_reason='PAYMENT_TIMEOUT', "
                + "version=version+1, updated_by=? WHERE id=? AND status='PENDING_PAYMENT' AND now() >= expires_at",
                actorId, orderId);
        if (n != 1) return false;
        for (Long itemId : orderItemIds(orderId)) items.releaseOrderOccupation(itemId, orderId, actorId, "PAYMENT_TIMEOUT");
        return true;
    }

    // =================================================================
    // 履约
    // =================================================================

    @Override @Transactional public OrderDetail selectPickupPoint(Long orderId, String idempotencyKey, SelectPickupPointRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        if (body == null || body.getPickupPointId() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        Map<String, Object> o = orderRow(orderId);
        if (!Long.valueOf(((Number) o.get("buyer_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);

        String status = (String) o.get("status");
        if ("DELIVERED".equals(status) || "COMPLETED".equals(status)) throw new DomainException(ErrorCode.ORDER_ALREADY_DELIVERED);
        if (!"PAID".equals(status)) throw new DomainException(ErrorCode.ORDER_NOT_PAID_FOR_PICKUP_SELECTION);

        Map<String, Object> pp;
        try { pp = jdbc.queryForMap("SELECT id, trade_zone_id, is_active FROM pickup_points WHERE id=?", body.getPickupPointId()); }
        catch (org.springframework.dao.EmptyResultDataAccessException ex) { throw new DomainException(ErrorCode.PICKUP_POINT_NOT_FOUND); }
        if (!Boolean.TRUE.equals(pp.get("is_active"))) throw new DomainException(ErrorCode.PICKUP_POINT_NOT_FOUND);
        if (((Number) pp.get("trade_zone_id")).longValue() != ((Number) o.get("trade_zone_id")).longValue()) {
            throw new DomainException(ErrorCode.PICKUP_POINT_ZONE_MISMATCH);
        }
        jdbc.update("UPDATE orders SET pickup_point_id=?, pickup_point_selected_at=now(), version=version+1, updated_by=? "
                + "WHERE id=? AND status='PAID' AND (pickup_point_id IS NULL OR pickup_point_id<>?)",
                body.getPickupPointId(), uid, orderId, body.getPickupPointId());
        return detail(orderId, uid, true);
    }

    @Override @Transactional public OrderDetail markOrderDelivered(Long orderId, String idempotencyKey) {
        Long uid = currentUserId();
        Map<String, Object> o = orderRow(orderId);
        if (!Long.valueOf(((Number) o.get("seller_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);

        String status = (String) o.get("status");
        if ("DELIVERED".equals(status) || "COMPLETED".equals(status)) return detail(orderId, uid, true); // IDEM-A
        if (!"PAID".equals(status)) throw new DomainException(ErrorCode.ORDER_STATUS_NOT_DELIVERABLE);
        if (o.get("pickup_point_id") == null) throw new DomainException(ErrorCode.ORDER_PICKUP_POINT_NOT_SELECTED);

        int n = jdbc.update("UPDATE orders SET status='DELIVERED', delivered_at=now(), version=version+1, updated_by=? "
                + "WHERE id=? AND status='PAID' AND pickup_point_id IS NOT NULL", uid, orderId);
        if (n != 1) {
            String st = (String) orderRow(orderId).get("status");
            if ("DELIVERED".equals(st) || "COMPLETED".equals(st)) return detail(orderId, uid, true);
            if ("PAID".equals(st)) throw new DomainException(ErrorCode.ORDER_PICKUP_POINT_NOT_SELECTED);
            throw new DomainException(ErrorCode.ORDER_STATUS_NOT_DELIVERABLE);
        }
        return detail(orderId, uid, true);
    }

    @Override @Transactional public OrderDetail confirmOrderReceipt(Long orderId, String idempotencyKey) {
        Long uid = currentUserId();
        Map<String, Object> o = orderRow(orderId);
        if (!Long.valueOf(((Number) o.get("buyer_id")).longValue()).equals(uid)) throw new DomainException(ErrorCode.FORBIDDEN);

        String status = (String) o.get("status");
        if ("COMPLETED".equals(status)) return detail(orderId, uid, true); // IDEM-A
        if (!"DELIVERED".equals(status)) throw new DomainException(ErrorCode.ORDER_STATUS_NOT_DELIVERED);

        int n = jdbc.update("UPDATE orders SET status='COMPLETED', completed_at=now(), "
                + "review_status='OPEN', review_deadline=now() + interval '7 days', version=version+1, updated_by=? "
                + "WHERE id=? AND status='DELIVERED'", uid, orderId);
        if (n != 1) {
            String st = (String) orderRow(orderId).get("status");
            if ("COMPLETED".equals(st)) return detail(orderId, uid, true);
            throw new DomainException(ErrorCode.ORDER_STATUS_NOT_DELIVERED);
        }
        for (Long itemId : orderItemIds(orderId)) items.markSold(itemId, uid);
        return detail(orderId, uid, true);
    }

    // =================================================================
    // 内部构造
    // =================================================================

    private OrderDetail detail(Long orderId, Long viewerId) { return detail(orderId, viewerId, true); }

    private OrderDetail detail(Long orderId, Long viewerId, boolean withItems) {
        Map<String, Object> o = orderRow(orderId);
        Long buyerId = ((Number) o.get("buyer_id")).longValue();
        Long sellerId = ((Number) o.get("seller_id")).longValue();
        boolean admin = isAdmin();
        if (!admin && !buyerId.equals(viewerId) && !sellerId.equals(viewerId)) throw new DomainException(ErrorCode.FORBIDDEN);

        OrderDetail d = new OrderDetail();
        d.setId(orderId);
        d.setOrderNo((String) o.get("order_no"));
        d.setStatus(OrderStatus.valueOf((String) o.get("status")));
        d.setRole(buyerId.equals(viewerId) ? "BUYER" : "SELLER");
        d.setEstimatedTotal(DecimalString.of((BigDecimal) o.get("estimated_total")));
        d.setPriceDisclaimer("用户自填估价，仅供参考，非平台定价");
        d.setBuyer(profile(o, buyerId, "buyer_nickname", "buyer_avatar", "buyer_created"));
        d.setSeller(profile(o, sellerId, "seller_nickname", "seller_avatar", "seller_created"));

        TradeZone zone = new TradeZone();
        zone.setId(((Number) o.get("trade_zone_id")).longValue());
        zone.setCode((String) o.get("zone_code"));
        zone.setName((String) o.get("zone_name"));
        d.setTradeZone(zone);

        if (o.get("pickup_point_id") != null) {
            PickupPoint pp = new PickupPoint();
            pp.setId(((Number) o.get("pickup_point_id")).longValue());
            pp.setCode((String) o.get("pickup_code"));
            pp.setName((String) o.get("pickup_name"));
            if (o.get("pickup_type") != null) pp.setPointType(PickupPointType.valueOf((String) o.get("pickup_type")));
            d.setPickupPoint(pp);
        }

        Timestamp expires = (Timestamp) o.get("expires_at");
        if (expires != null) {
            d.setExpiresAt(expires.toInstant());
            boolean pending = "PENDING_PAYMENT".equals(o.get("status"));
            d.setRemainingSeconds(pending ? Math.max(0, Duration.between(Instant.now(), expires.toInstant()).getSeconds()) : null);
        }
        d.setPaidAt(instant(o.get("paid_at")));
        d.setDeliveredAt(instant(o.get("delivered_at")));
        d.setCompletedAt(instant(o.get("completed_at")));
        d.setCancelledAt(instant(o.get("cancelled_at")));
        if (o.get("cancel_reason") != null) d.setCancelReason(OrderCancelReason.valueOf((String) o.get("cancel_reason")));
        d.setCancelNote((String) o.get("cancel_note"));
        if (o.get("review_status") != null) d.setReviewStatus(ReviewStatus.valueOf((String) o.get("review_status")));
        d.setReviewDeadline(instant(o.get("review_deadline")));
        d.setCanReview(Boolean.TRUE.equals(o.get("can_review")));
        d.setVersion(((Number) o.get("version")).intValue());
        d.setCreatedAt(instant(o.get("created_at")));
        if (withItems) d.setItems(orderItems(orderId));
        return d;
    }

    private List<OrderItem> orderItems(Long orderId) {
        return jdbc.query("SELECT oi.*, (SELECT image_url FROM item_images im WHERE im.item_id=oi.item_id ORDER BY sort_order,id LIMIT 1) cover "
                        + "FROM order_items oi WHERE oi.order_id=? ORDER BY oi.id",
                (rs, i) -> {
                    OrderItem oi = new OrderItem();
                    oi.setId(rs.getLong("id"));
                    oi.setItemId(rs.getLong("item_id"));
                    oi.setTitleSnapshot(rs.getString("title_snapshot"));
                    oi.setPriceSnapshot(DecimalString.of(rs.getBigDecimal("price_snapshot")));
                    oi.setConditionSnapshot(rs.getString("condition_snapshot"));
                    oi.setCoverImageUrl(rs.getString("cover"));
                    return oi;
                }, orderId);
    }

    private OrderSummary summaryFrom(java.sql.ResultSet rs, Long viewerId) throws java.sql.SQLException {
        OrderSummary s = new OrderSummary();
        s.setId(rs.getLong("id"));
        s.setOrderNo(rs.getString("order_no"));
        s.setStatus(OrderStatus.valueOf(rs.getString("status")));
        Long buyerId = rs.getLong("buyer_id");
        Long sellerId = rs.getLong("seller_id");
        s.setRole(buyerId.equals(viewerId) ? "BUYER" : "SELLER");
        s.setEstimatedTotal(DecimalString.of(rs.getBigDecimal("estimated_total")));
        s.setTradeZoneId(rs.getLong("trade_zone_id"));
        s.setPickupPointName(rs.getString("pickup_name"));
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) {
            s.setExpiresAt(expires.toInstant());
            if ("PENDING_PAYMENT".equals(rs.getString("status"))) {
                s.setRemainingSeconds(Math.max(0, Duration.between(Instant.now(), expires.toInstant()).getSeconds()));
            }
        }
        s.setCreatedAt(instant(rs.getTimestamp("created_at")));
        s.setItemCount(rs.getInt("item_count"));
        s.setCoverImageUrl(rs.getString("cover"));

        UserProfile counterparty = new UserProfile();
        if (buyerId.equals(viewerId)) {
            counterparty.setId(sellerId);
            counterparty.setNickname(rs.getString("seller_nickname"));
            counterparty.setAvatarUrl(rs.getString("seller_avatar"));
            counterparty.setCreatedAt(instant(rs.getTimestamp("seller_created")));
        } else {
            counterparty.setId(buyerId);
            counterparty.setNickname(rs.getString("buyer_nickname"));
            counterparty.setAvatarUrl(rs.getString("buyer_avatar"));
            counterparty.setCreatedAt(instant(rs.getTimestamp("buyer_created")));
        }
        s.setCounterparty(counterparty);
        return s;
    }

    private UserProfile profile(Map<String, Object> o, Long id, String nickKey, String avatarKey, String createdKey) {
        UserProfile p = new UserProfile();
        p.setId(id);
        p.setNickname((String) o.get(nickKey));
        p.setAvatarUrl((String) o.get(avatarKey));
        p.setCreatedAt(instant(o.get(createdKey)));
        return p;
    }

    private PaymentResult paymentResult(Map<String, Object> o) {
        PaymentResult r = new PaymentResult();
        r.setOrderId(((Number) o.get("id")).longValue());
        r.setOrderNo((String) o.get("order_no"));
        r.setStatus(OrderStatus.valueOf((String) o.get("status")));
        r.setAmount(DecimalString.of((BigDecimal) o.get("estimated_total")));
        r.setChannel("MOCK");
        r.setPaidAt(instant(o.get("paid_at")));
        r.setMockNotice("模拟付款，未产生真实扣款");
        return r;
    }

    private Map<String, Object> orderRow(Long orderId) {
        try { return jdbc.queryForMap(ORDER_SELECT + "WHERE o.id=?", orderId); }
        catch (org.springframework.dao.EmptyResultDataAccessException ex) { throw new DomainException(ErrorCode.ORDER_NOT_FOUND); }
    }

    private List<Long> orderItemIds(Long orderId) {
        return jdbc.queryForList("SELECT item_id FROM order_items WHERE order_id=? ORDER BY id", Long.class, orderId);
    }

    private void ensureZoneSelected(Long uid, Long zoneId) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM user_trade_zones WHERE user_id=? AND trade_zone_id=?", Long.class, uid, zoneId);
        if (n == null || n == 0) throw new DomainException(ErrorCode.TRADE_ZONE_NOT_SELECTED);
    }

    private void ensureNotRestricted(Long uid) {
        String status = jdbc.queryForObject("SELECT status::text FROM users WHERE id=?", String.class, uid);
        if ("RESTRICTED".equals(status)) throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser)) throw new DomainException(ErrorCode.UNAUTHORIZED);
        return ((AuthenticatedUser) auth.getPrincipal()).getUserId();
    }

    private Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant();
        return null;
    }
}
