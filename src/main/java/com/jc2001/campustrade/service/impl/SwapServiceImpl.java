package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.*;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.security.AuthenticatedUser;
import com.jc2001.campustrade.service.IdempotencyService;
import com.jc2001.campustrade.service.ItemService;
import com.jc2001.campustrade.service.SwapService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交换包与交换提议实现（切片 3B，FR-402 ~ FR-412）。
 *
 * <p>关键约束：
 * <ul>
 *   <li>R-21 交换包 1–5 件；SB-03 同一物品同时只能在一个进行中交换包（部分唯一索引兜底）</li>
 *   <li>R-22 / OOS-04 整包接受，不支持部分接受</li>
 *   <li>R-23 交换占用与订单占用互斥（{@code item_occupations} 条件更新）</li>
 *   <li>R-25 提议 72 小时过期，全部释放</li>
 *   <li>R-26 交换码一次性核销（条件更新）</li>
 *   <li>R-27 双方各确认一次，齐备才完成</li>
 *   <li>R-28 反提案仅一轮（{@code counter_depth ≤ 1}）</li>
 * </ul>
 */
@Service
public class SwapServiceImpl implements SwapService {

    private static final int MAX_BUNDLE_ITEMS = 5;
    private static final Duration PROPOSAL_TTL = Duration.ofHours(72);
    private static final Duration REVIEW_WINDOW = Duration.ofDays(7);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private static final String BUNDLE_SELECT =
            "SELECT b.*, u.nickname owner_nickname, u.avatar_url owner_avatar, u.created_at owner_created, "
            + "z.code zone_code, z.name zone_name "
            + "FROM swap_bundles b JOIN users u ON u.id=b.owner_id JOIN trade_zones z ON z.id=b.trade_zone_id ";

    private static final String PROPOSAL_SELECT =
            "SELECT p.*, z.code zone_code, z.name zone_name, "
            + "pu.nickname proposer_nickname, pu.avatar_url proposer_avatar, pu.created_at proposer_created, "
            + "tu.nickname target_nickname, tu.avatar_url target_avatar, tu.created_at target_created "
            + "FROM swap_proposals p "
            + "JOIN trade_zones z ON z.id=p.trade_zone_id "
            + "JOIN users pu ON pu.id=p.proposer_id "
            + "JOIN users tu ON tu.id=p.target_id ";

    private final JdbcTemplate jdbc;
    private final ItemService items;
    private final IdempotencyService idempotency;

    public SwapServiceImpl(JdbcTemplate jdbc, ItemService items, IdempotencyService idempotency) {
        this.jdbc = jdbc;
        this.items = items;
        this.idempotency = idempotency;
    }

    // =================================================================
    // 交换包
    // =================================================================

    @Override @Transactional public SwapBundleDetail createSwapBundle(String idempotencyKey, CreateSwapBundleRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "createSwapBundle", body, SwapBundleDetail.class,
                () -> createBundleOnce(uid, body));
    }

    private SwapBundleDetail createBundleOnce(Long uid, CreateSwapBundleRequest body) {
        ensureNotRestricted(uid);
        if (body == null || body.getItemIds() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        List<Long> itemIds = distinct(body.getItemIds());
        if (itemIds.size() < 1 || itemIds.size() > MAX_BUNDLE_ITEMS) {
            throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE);
        }
        if (body.getTradeZoneId() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        ensureZoneSelected(uid, body.getTradeZoneId());

        BigDecimal total = BigDecimal.ZERO;
        for (Long itemId : itemIds) {
            Map<String, Object> it = itemRow(itemId);
            if (((Number) it.get("seller_id")).longValue() != uid) throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_NOT_OWNED);
            requireInSale(it);
            if (((Number) it.get("trade_zone_id")).longValue() != body.getTradeZoneId().longValue()) {
                throw new DomainException(ErrorCode.TRADE_ZONE_MISMATCH);
            }
            total = total.add((BigDecimal) it.get("estimated_price"));
        }

        Long bundleId;
        try {
            bundleId = jdbc.queryForObject("INSERT INTO swap_bundles(owner_id, trade_zone_id, title, status, estimated_total, packed_at, created_by) "
                    + "VALUES (?,?,?,'PACKED',?::numeric,now(),?) RETURNING id",
                    Long.class, uid, body.getTradeZoneId(), body.getTitle(), total.toPlainString(), uid);
            for (Long itemId : itemIds) {
                Map<String, Object> it = itemRow(itemId);
                jdbc.update("INSERT INTO swap_bundle_items(swap_bundle_id, item_id, item_status, price_snapshot, created_by) "
                        + "VALUES (?,?,'ACTIVE',?::numeric,?) "
                        + "ON CONFLICT (swap_bundle_id, item_id) DO UPDATE SET item_status='ACTIVE', released_at=NULL, "
                        + "release_reason=NULL, price_snapshot=EXCLUDED.price_snapshot, updated_by=EXCLUDED.created_by", bundleId, itemId,
                        ((BigDecimal) it.get("estimated_price")).toPlainString(), uid);
            }
        } catch (DataIntegrityViolationException ex) {
            // uq_swap_bundle_items_active：物品已在其他进行中交换包
            throw new DomainException(ErrorCode.SWAP_ITEM_IN_ACTIVE_BUNDLE);
        }
        return bundleDetail(bundleId, uid);
    }

    @Override public PageResult<SwapBundleSummary> listMySwapBundles(Integer page, Integer size, List<SwapBundleStatus> status) {
        Long uid = currentUserId();
        int p = page == null ? 0 : page, n = size == null ? 20 : size;
        if (p < 0 || n < 1 || n > 100) throw new DomainException(ErrorCode.VALIDATION_ERROR);

        StringBuilder where = new StringBuilder(" WHERE b.owner_id=? AND b.deleted_at IS NULL");
        List<Object> args = new ArrayList<>(Collections.singletonList(uid));
        if (status != null && !status.isEmpty()) {
            where.append(" AND b.status::text IN (");
            for (int i = 0; i < status.size(); i++) { if (i > 0) where.append(','); where.append('?'); args.add(status.get(i).name()); }
            where.append(')');
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM swap_bundles b" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(n); pageArgs.add(p * n);
        List<SwapBundleSummary> rows = jdbc.query(BUNDLE_SELECT + where + " ORDER BY b.created_at DESC, b.id DESC LIMIT ? OFFSET ?",
                (rs, i) -> bundleSummary(rs), pageArgs.toArray());
        return PageResult.of(p, n, total == null ? 0 : total, rows);
    }

    @Override public SwapBundleDetail getSwapBundle(Long bundleId) {
        Long uid = currentUserId();
        return bundleDetail(bundleId, uid);
    }

    @Override @Transactional public SwapBundleDetail updateSwapBundle(Long bundleId, String idempotencyKey, UpdateSwapBundleRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> b = ownedBundle(bundleId, uid);
        String status = (String) b.get("status");
        if ("IN_SWAP".equals(status) || "OCCUPIED".equals(status)) throw new DomainException(ErrorCode.SWAP_BUNDLE_LOCKED);
        if ("COMPLETED".equals(status)) throw new DomainException(ErrorCode.SWAP_BUNDLE_STATUS_NOT_EDITABLE);
        if (body == null || body.getItemIds() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        List<Long> itemIds = distinct(body.getItemIds());
        if (itemIds.size() < 1 || itemIds.size() > MAX_BUNDLE_ITEMS) {
            throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE);
        }
        int version = body.getVersion() == null ? -1 : body.getVersion();
        if (version < 0) throw new DomainException(ErrorCode.VALIDATION_ERROR);

        BigDecimal total = BigDecimal.ZERO;
        for (Long itemId : itemIds) {
            Map<String, Object> it = itemRow(itemId);
            if (((Number) it.get("seller_id")).longValue() != uid) throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_NOT_OWNED);
            requireInSale(it);
            total = total.add((BigDecimal) it.get("estimated_price"));
        }
        int n = jdbc.update("UPDATE swap_bundles SET title=COALESCE(?,title), estimated_total=?::numeric, status='PACKED', "
                + "packed_at=COALESCE(packed_at, now()), version=version+1, updated_by=? WHERE id=? AND owner_id=? AND version=?",
                body.getTitle(), total.toPlainString(), uid, bundleId, uid, version);
        if (n != 1) throw new DomainException(ErrorCode.CONFLICT);

        jdbc.update("UPDATE swap_bundle_items SET item_status='RELEASED', released_at=now(), release_reason='BUNDLE_EDITED', updated_by=? "
                + "WHERE swap_bundle_id=? AND item_status='ACTIVE'", uid, bundleId);
        try {
            for (Long itemId : itemIds) {
                Map<String, Object> it = itemRow(itemId);
                jdbc.update("INSERT INTO swap_bundle_items(swap_bundle_id, item_id, item_status, price_snapshot, created_by) "
                        + "VALUES (?,?,'ACTIVE',?::numeric,?) "
                        + "ON CONFLICT (swap_bundle_id, item_id) DO UPDATE SET item_status='ACTIVE', released_at=NULL, "
                        + "release_reason=NULL, price_snapshot=EXCLUDED.price_snapshot, updated_by=EXCLUDED.created_by", bundleId, itemId,
                        ((BigDecimal) it.get("estimated_price")).toPlainString(), uid);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(ErrorCode.SWAP_ITEM_IN_ACTIVE_BUNDLE);
        }
        return bundleDetail(bundleId, uid);
    }

    @Override @Transactional public Void deleteSwapBundle(Long bundleId) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> b = bundleRow(bundleId);
        if (((Number) b.get("owner_id")).longValue() != uid.longValue()) throw new DomainException(ErrorCode.FORBIDDEN);
        if (b.get("deleted_at") != null) return null; // 幂等：重复删除返回 204
        String status = (String) b.get("status");
        if ("IN_SWAP".equals(status) || "OCCUPIED".equals(status)) throw new DomainException(ErrorCode.SWAP_BUNDLE_LOCKED);
        jdbc.update("UPDATE swap_bundles SET deleted_at=now(), version=version+1, updated_by=? "
                + "WHERE id=? AND owner_id=? AND deleted_at IS NULL", uid, bundleId, uid);
        jdbc.update("UPDATE swap_bundle_items SET item_status='RELEASED', released_at=now(), release_reason='BUNDLE_DELETED', updated_by=? "
                + "WHERE swap_bundle_id=? AND item_status='ACTIVE'", uid, bundleId);
        return null;
    }

    // =================================================================
    // 交换提议
    // =================================================================

    @Override @Transactional public SwapProposalDetail createSwapProposal(String idempotencyKey, CreateSwapProposalRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "createSwapProposal", body, SwapProposalDetail.class,
                () -> createProposalOnce(uid, body));
    }

    private SwapProposalDetail createProposalOnce(Long uid, CreateSwapProposalRequest body) {
        ensureNotRestricted(uid);
        if (body == null || body.getProposerBundleId() == null || body.getTargetItemIds() == null
                || body.getTargetItemIds().isEmpty()) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        Map<String, Object> bundle = ownedBundle(body.getProposerBundleId(), uid);
        if (!"PACKED".equals(bundle.get("status"))) throw new DomainException(ErrorCode.SWAP_BUNDLE_LOCKED);

        List<Long> targetIds = distinct(body.getTargetItemIds());
        Long targetId = null;
        BigDecimal wantTotal = BigDecimal.ZERO;
        for (Long itemId : targetIds) {
            Map<String, Object> it = itemRow(itemId);
            Long sellerId = ((Number) it.get("seller_id")).longValue();
            if (targetId == null) targetId = sellerId;
            else if (!targetId.equals(sellerId)) throw new DomainException(ErrorCode.ORDER_ITEM_SELLER_MISMATCH);
            if (targetId.equals(uid)) throw new DomainException(ErrorCode.SWAP_SELF_PROPOSAL_NOT_ALLOWED);
            if (!"ACCEPTING".equals(it.get("swap_preference"))) throw new DomainException(ErrorCode.SWAP_TARGET_NOT_ACCEPTING);
            if (!Objects.equals(((Number) it.get("trade_zone_id")).longValue(), ((Number) bundle.get("trade_zone_id")).longValue())) {
                throw new DomainException(ErrorCode.TRADE_ZONE_MISMATCH);
            }
            requireInSale(it);
            wantTotal = wantTotal.add((BigDecimal) it.get("estimated_price"));
        }

        List<Map<String, Object>> bundleItems = jdbc.queryForList(
                "SELECT sbi.item_id, i.title, i.estimated_price FROM swap_bundle_items sbi JOIN items i ON i.id=sbi.item_id "
                + "WHERE sbi.swap_bundle_id=? AND sbi.item_status='ACTIVE' ORDER BY sbi.id", body.getProposerBundleId());
        if (bundleItems.isEmpty() || bundleItems.size() > MAX_BUNDLE_ITEMS) {
            throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE);
        }
        BigDecimal offerTotal = BigDecimal.ZERO;
        for (Map<String, Object> bi : bundleItems) {
            Map<String, Object> it = itemRow(((Number) bi.get("item_id")).longValue());
            requireInSale(it);
            offerTotal = offerTotal.add((BigDecimal) it.get("estimated_price"));
        }

        Instant expiresAt = Instant.now().plus(PROPOSAL_TTL);
        Long proposalId = jdbc.queryForObject(
                "INSERT INTO swap_proposals(proposal_no, proposer_id, target_id, proposer_bundle_id, trade_zone_id, status, "
                + "counter_depth, message, estimated_total_offer, estimated_total_want, expires_at, created_by) "
                + "VALUES (?,?,?,?,?,'PENDING',0,?,?::numeric,?::numeric,?,?) RETURNING id",
                Long.class, nextNo(), uid, targetId, body.getProposerBundleId(), bundle.get("trade_zone_id"),
                body.getMessage(), offerTotal.toPlainString(), wantTotal.toPlainString(), Timestamp.from(expiresAt), uid);

        for (Map<String, Object> bi : bundleItems) {
            Long itemId = ((Number) bi.get("item_id")).longValue();
            jdbc.update("INSERT INTO swap_proposal_items(swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by) "
                    + "VALUES (?,?,?,?::numeric,?)", proposalId, itemId, bi.get("title"),
                    ((BigDecimal) bi.get("estimated_price")).toPlainString(), uid);
        }
        for (Long itemId : targetIds) {
            Map<String, Object> it = itemRow(itemId);
            jdbc.update("INSERT INTO swap_proposal_items(swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by) "
                    + "VALUES (?,?,?,?::numeric,?)", proposalId, itemId, it.get("title"),
                    ((BigDecimal) it.get("estimated_price")).toPlainString(), uid);
        }

        // R-16 / R-23：占用双方全部商品（条件更新，冲突即整笔回滚）
        for (Map<String, Object> bi : bundleItems) items.occupyForSwap(((Number) bi.get("item_id")).longValue(), proposalId, uid);
        for (Long itemId : targetIds) items.occupyForSwap(itemId, proposalId, uid);
        jdbc.update("UPDATE swap_bundles SET status='IN_SWAP', version=version+1, updated_by=? WHERE id=? AND status='PACKED'",
                uid, body.getProposerBundleId());
        return proposalDetail(proposalId, uid);
    }

    @Override public PageResult<SwapProposalSummary> listSwapProposals(Integer page, Integer size, List<String> sort, String role, List<SwapProposalStatus> status) {
        Long uid = currentUserId();
        int p = page == null ? 0 : page, n = size == null ? 20 : size;
        if (p < 0 || n < 1 || n > 100) throw new DomainException(ErrorCode.VALIDATION_ERROR);

        String order = "p.created_at DESC,p.id DESC";
        if (sort != null && !sort.isEmpty()) {
            List<String> terms = new ArrayList<>();
            for (String s : sort) {
                String[] a = s.split(",");
                if (!"created_at".equals(a[0])) throw new DomainException(ErrorCode.COMMON_INVALID_SORT_FIELD);
                String dir = a.length > 1 ? a[1].toUpperCase(Locale.ROOT) : "ASC";
                if (!"ASC".equals(dir) && !"DESC".equals(dir)) throw new DomainException(ErrorCode.VALIDATION_ERROR);
                terms.add("p.created_at " + dir);
            }
            order = String.join(",", terms);
        }
        StringBuilder where = new StringBuilder(" WHERE (p.proposer_id=? OR p.target_id=?)");
        List<Object> args = new ArrayList<>(Arrays.asList(uid, uid));
        String r = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        if (r != null && !r.isEmpty()) {
            if ("PROPOSER".equals(r)) { where.append(" AND p.proposer_id=?"); args.add(uid); }
            else if ("TARGET".equals(r)) { where.append(" AND p.target_id=?"); args.add(uid); }
            else throw new DomainException(ErrorCode.VALIDATION_ERROR);
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND p.status::text IN (");
            for (int i = 0; i < status.size(); i++) { if (i > 0) where.append(','); where.append('?'); args.add(status.get(i).name()); }
            where.append(')');
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM swap_proposals p" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(n); pageArgs.add(p * n);
        List<SwapProposalSummary> rows = jdbc.query(PROPOSAL_SELECT + where + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                (rs, i) -> proposalSummary(rs, uid), pageArgs.toArray());
        return PageResult.of(p, n, total == null ? 0 : total, rows);
    }

    @Override public SwapProposalDetail getSwapProposal(Long proposalId) {
        return proposalDetail(proposalId, currentUserId());
    }

    @Override @Transactional public SwapProposalDetail acceptSwapProposal(Long proposalId, String idempotencyKey) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "acceptSwapProposal", proposalId, SwapProposalDetail.class,
                () -> acceptOnce(uid, proposalId));
    }

    private SwapProposalDetail acceptOnce(Long uid, Long proposalId) {
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        if (((Number) p.get("target_id")).longValue() != uid) throw new DomainException(ErrorCode.FORBIDDEN);
        String status = (String) p.get("status");
        if ("ACCEPTED".equals(status) || "COMPLETED".equals(status)) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_ACCEPTED);
        if (!"PENDING".equals(status)) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);

        int n = jdbc.update("UPDATE swap_proposals SET status='ACCEPTED', accepted_at=now(), version=version+1, updated_by=? "
                + "WHERE id=? AND status='PENDING'", uid, proposalId);
        if (n != 1) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);

        issueCode(proposalId, uid);
        jdbc.update("UPDATE swap_bundles SET status='OCCUPIED', version=version+1, updated_by=? WHERE id=? AND status='IN_SWAP'",
                uid, p.get("proposer_bundle_id"));
        return proposalDetail(proposalId, uid);
    }

    private void issueCode(Long proposalId, Long actor) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM swap_codes WHERE swap_proposal_id=?", Integer.class, proposalId);
        if (existing != null && existing > 0) return; // C-05：一提议一码，重复接受不重复生成
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
            try {
                jdbc.update("INSERT INTO swap_codes(swap_proposal_id, code, status, expires_at, created_by) "
                        + "VALUES (?,?,'ISSUED', now() + interval '72 hours', ?)", proposalId, code, actor);
                return;
            } catch (DataIntegrityViolationException ex) {
                // 码冲突，重试
            }
        }
        throw new DomainException(ErrorCode.CONFLICT);
    }

    @Override @Transactional public SwapProposalDetail rejectSwapProposal(Long proposalId, String idempotencyKey, RejectSwapProposalRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        if (((Number) p.get("target_id")).longValue() != uid) throw new DomainException(ErrorCode.FORBIDDEN);
        String status = (String) p.get("status");
        if ("REJECTED".equals(status)) return proposalDetail(proposalId, uid); // IDEM-A
        if (!"PENDING".equals(status)) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);

        int n = jdbc.update("UPDATE swap_proposals SET status='REJECTED', termination_reason='TARGET_REJECTED', "
                + "version=version+1, updated_by=? WHERE id=? AND status='PENDING'", uid, proposalId);
        if (n != 1) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);
        releaseAll(proposalId, uid, "TARGET_REJECTED");
        return proposalDetail(proposalId, uid);
    }

    @Override @Transactional public SwapProposalDetail counterSwapProposal(Long proposalId, String idempotencyKey, CounterSwapProposalRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "counterSwapProposal", body, SwapProposalDetail.class,
                () -> counterOnce(uid, proposalId, body));
    }

    private SwapProposalDetail counterOnce(Long uid, Long proposalId, CounterSwapProposalRequest body) {
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        if (((Number) p.get("target_id")).longValue() != uid) throw new DomainException(ErrorCode.FORBIDDEN);
        if (((Number) p.get("counter_depth")).intValue() >= 1) throw new DomainException(ErrorCode.SWAP_COUNTER_NOT_ALLOWED);
        if (!"PENDING".equals((String) p.get("status"))) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);
        if (body == null || body.getTargetBundleId() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);

        // 原提议终结并释放
        int n = jdbc.update("UPDATE swap_proposals SET status='COUNTERED', termination_reason='COUNTERED', "
                + "version=version+1, updated_by=? WHERE id=? AND status='PENDING' AND counter_depth=0", uid, proposalId);
        if (n != 1) throw new DomainException(ErrorCode.SWAP_COUNTER_NOT_ALLOWED);
        releaseAll(proposalId, uid, "COUNTERED");

        // 新提议：proposer=原 target，target=原 proposer，want=原 offer 商品
        List<Long> wantIds = jdbc.queryForList("SELECT item_id FROM swap_proposal_items WHERE swap_proposal_id=? ORDER BY id",
                Long.class, proposalId);
        Map<String, Object> newBundle = ownedBundle(body.getTargetBundleId(), uid);
        if (!"PACKED".equals(newBundle.get("status"))) throw new DomainException(ErrorCode.SWAP_BUNDLE_LOCKED);
        List<Map<String, Object>> bundleItems = jdbc.queryForList(
                "SELECT sbi.item_id, sbi.price_snapshot FROM swap_bundle_items sbi WHERE sbi.swap_bundle_id=? AND sbi.item_status='ACTIVE' ORDER BY sbi.id",
                body.getTargetBundleId());
        if (bundleItems.isEmpty() || bundleItems.size() > MAX_BUNDLE_ITEMS) throw new DomainException(ErrorCode.SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE);

        BigDecimal offerTotal = BigDecimal.ZERO, wantTotal = BigDecimal.ZERO;
        for (Long itemId : wantIds) {
            Map<String, Object> it = itemRow(itemId);
            requireInSale(it);
            wantTotal = wantTotal.add((BigDecimal) it.get("estimated_price"));
        }
        for (Map<String, Object> bi : bundleItems) {
            Map<String, Object> it = itemRow(((Number) bi.get("item_id")).longValue());
            requireInSale(it);
            offerTotal = offerTotal.add((BigDecimal) bi.get("price_snapshot"));
        }

        Long newId = jdbc.queryForObject(
                "INSERT INTO swap_proposals(proposal_no, proposer_id, target_id, proposer_bundle_id, target_bundle_id, parent_proposal_id, trade_zone_id, "
                + "status, counter_depth, message, estimated_total_offer, estimated_total_want, expires_at, created_by) "
                + "VALUES (?,?,?,?,?,?,?,'PENDING',1,?,?::numeric,?::numeric,?,?) RETURNING id",
                Long.class, nextNo(), uid, p.get("proposer_id"), body.getTargetBundleId(), p.get("proposer_bundle_id"), proposalId,
                p.get("trade_zone_id"), body.getMessage(), offerTotal.toPlainString(), wantTotal.toPlainString(),
                Timestamp.from(Instant.now().plus(PROPOSAL_TTL)), uid);

        for (Map<String, Object> bi : bundleItems) {
            Long itemId = ((Number) bi.get("item_id")).longValue();
            Map<String, Object> it = itemRow(itemId);
            jdbc.update("INSERT INTO swap_proposal_items(swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by) "
                    + "VALUES (?,?,?,?::numeric,?)", newId, itemId, it.get("title"),
                    ((BigDecimal) bi.get("price_snapshot")).toPlainString(), uid);
            items.occupyForSwap(itemId, newId, uid);
        }
        for (Long itemId : wantIds) {
            Map<String, Object> it = itemRow(itemId);
            jdbc.update("INSERT INTO swap_proposal_items(swap_proposal_id, item_id, title_snapshot, price_snapshot, created_by) "
                    + "VALUES (?,?,?,?::numeric,?)", newId, itemId, it.get("title"),
                    ((BigDecimal) it.get("estimated_price")).toPlainString(), uid);
            items.occupyForSwap(itemId, newId, uid);
        }
        jdbc.update("UPDATE swap_bundles SET status='IN_SWAP', version=version+1, updated_by=? WHERE id=? AND status='PACKED'",
                uid, body.getTargetBundleId());
        return proposalDetail(newId, uid);
    }

    @Override @Transactional public SwapProposalDetail cancelSwapProposal(Long proposalId, String idempotencyKey) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        if (((Number) p.get("proposer_id")).longValue() != uid) throw new DomainException(ErrorCode.FORBIDDEN);
        String status = (String) p.get("status");
        if ("CANCELLED".equals(status)) return proposalDetail(proposalId, uid); // IDEM-A
        if (!"PENDING".equals(status)) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);
        int n = jdbc.update("UPDATE swap_proposals SET status='CANCELLED', termination_reason='PROPOSER_CANCELLED', "
                + "version=version+1, updated_by=? WHERE id=? AND status='PENDING'", uid, proposalId);
        if (n != 1) throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);
        releaseAll(proposalId, uid, "PROPOSER_CANCELLED");
        return proposalDetail(proposalId, uid);
    }

    // =================================================================
    // 交换码 / 确认 / 评价
    // =================================================================

    @Override public SwapCode getSwapCode(Long proposalId) {
        Long uid = currentUserId();
        Map<String, Object> p = proposalRow(proposalId);
        requireParticipant(p, uid);
        String status = (String) p.get("status");
        if (!"ACCEPTED".equals(status) && !"COMPLETED".equals(status)) throw new DomainException(ErrorCode.SWAP_CODE_NOT_ISSUED);
        return codeRow(proposalId, uid);
    }

    @Override @Transactional public SwapCode verifySwapCode(Long proposalId, String idempotencyKey, VerifySwapCodeRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        if (body == null || body.getCode() == null) throw new DomainException(ErrorCode.VALIDATION_ERROR);
        Map<String, Object> p = proposalRow(proposalId);
        requireParticipant(p, uid);
        if (!"ACCEPTED".equals(p.get("status")) && !"COMPLETED".equals(p.get("status"))) {
            throw new DomainException(ErrorCode.SWAP_CODE_NOT_ISSUED);
        }
        Map<String, Object> c = codeMap(proposalId);
        if (c == null) throw new DomainException(ErrorCode.SWAP_CODE_NOT_ISSUED);
        if ("VERIFIED".equals(c.get("status"))) throw new DomainException(ErrorCode.SWAP_CODE_ALREADY_VERIFIED);
        if (!String.valueOf(c.get("code")).equals(body.getCode().trim())) throw new DomainException(ErrorCode.SWAP_CODE_INVALID);

        int n = jdbc.update("UPDATE swap_codes SET status='VERIFIED', verified_at=now(), verified_by=?, version=version+1, updated_by=? "
                + "WHERE swap_proposal_id=? AND status='ISSUED'", uid, uid, proposalId);
        if (n != 1) throw new DomainException(ErrorCode.SWAP_CODE_ALREADY_VERIFIED);
        return codeRow(proposalId, uid);
    }

    @Override @Transactional public SwapProposalDetail confirmSwap(Long proposalId, String idempotencyKey, ConfirmSwapRequest body) {
        Long uid = currentUserId();
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        requireParticipant(p, uid);
        if (!"ACCEPTED".equals(p.get("status")) && !"COMPLETED".equals(p.get("status"))) {
            throw new DomainException(ErrorCode.SWAP_PROPOSAL_ALREADY_FINALIZED);
        }
        Map<String, Object> c = codeMap(proposalId);
        if (c == null || !"VERIFIED".equals(c.get("status"))) throw new DomainException(ErrorCode.SWAP_CODE_NOT_VERIFIED);

        boolean isProposer = ((Number) p.get("proposer_id")).longValue() == uid;
        if (isProposer) {
            jdbc.update("UPDATE swap_proposals SET proposer_confirmed_at=COALESCE(proposer_confirmed_at, now()), "
                    + "version=version+1, updated_by=? WHERE id=?", uid, proposalId);
        } else {
            jdbc.update("UPDATE swap_proposals SET target_confirmed_at=COALESCE(target_confirmed_at, now()), "
                    + "version=version+1, updated_by=? WHERE id=?", uid, proposalId);
        }

        Map<String, Object> now = proposalRow(proposalId);
        if (now.get("proposer_confirmed_at") != null && now.get("target_confirmed_at") != null
                && !"COMPLETED".equals(now.get("status"))) {
            int n = jdbc.update("UPDATE swap_proposals SET status='COMPLETED', completed_at=now(), "
                    + "version=version+1, updated_by=? WHERE id=? AND status='ACCEPTED'", uid, proposalId);
            if (n == 1) {
                for (Long itemId : proposalItemIds(proposalId)) items.markSwapSold(itemId, uid);
                jdbc.update("UPDATE swap_bundles SET status='COMPLETED', completed_at=now(), version=version+1, updated_by=? "
                        + "WHERE id=?", uid, now.get("proposer_bundle_id"));
                jdbc.update("UPDATE swap_bundle_items SET item_status='COMPLETED', released_at=now(), release_reason='SWAP_COMPLETED', updated_by=? "
                        + "WHERE swap_bundle_id=? AND item_status='ACTIVE'", uid, now.get("proposer_bundle_id"));
            }
        }
        return proposalDetail(proposalId, uid);
    }

    @Override @Transactional public Review createSwapReview(Long proposalId, String idempotencyKey, CreateReviewRequest body) {
        Long uid = currentUserId();
        return idempotency.execute(uid, idempotencyKey, "createSwapReview", body, Review.class,
                () -> createReviewOnce(uid, proposalId, body));
    }

    private Review createReviewOnce(Long uid, Long proposalId, CreateReviewRequest body) {
        ensureNotRestricted(uid);
        Map<String, Object> p = proposalRow(proposalId);
        requireParticipant(p, uid);
        if (!"COMPLETED".equals(p.get("status"))) throw new DomainException(ErrorCode.REVIEW_TRANSACTION_NOT_COMPLETED);
        Timestamp completed = (Timestamp) p.get("completed_at");
        if (completed == null || completed.toInstant().plus(REVIEW_WINDOW).isBefore(Instant.now())) {
            throw new DomainException(ErrorCode.REVIEW_WINDOW_CLOSED);
        }
        if (body == null || body.getRating() == null || body.getRating() < 1 || body.getRating() > 5) {
            throw new DomainException(ErrorCode.REVIEW_RATING_OUT_OF_RANGE);
        }
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM reviews WHERE swap_proposal_id=? AND reviewer_id=?",
                Integer.class, proposalId, uid);
        if (existing != null && existing > 0) throw new DomainException(ErrorCode.REVIEW_ALREADY_SUBMITTED);

        boolean isProposer = ((Number) p.get("proposer_id")).longValue() == uid;
        Long revieweeId = isProposer ? ((Number) p.get("target_id")).longValue() : ((Number) p.get("proposer_id")).longValue();
        String role = isProposer ? "BUYER" : "SELLER";
        Long reviewId = jdbc.queryForObject(
                "INSERT INTO reviews(swap_proposal_id, reviewer_id, reviewee_id, reviewer_role, rating, content, created_by) "
                + "VALUES (?,?,?,?::review_role,?,?,?) RETURNING id",
                Long.class, proposalId, uid, revieweeId, role, body.getRating(), body.getContent(), uid);
        return reviewRow(reviewId);
    }

    // =================================================================
    // 内部任务：72 小时过期
    // =================================================================

    @Transactional public int expireProposals() {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM swap_proposals WHERE status='PENDING' AND now() >= expires_at ORDER BY id LIMIT 500", Long.class);
        int processed = 0;
        for (Long id : ids) {
            int n = jdbc.update("UPDATE swap_proposals SET status='EXPIRED', termination_reason='EXPIRED_72H', "
                    + "version=version+1 WHERE id=? AND status='PENDING' AND now() >= expires_at", id);
            if (n != 1) continue;
            releaseAll(id, 0L, "EXPIRED_72H");
            processed++;
        }
        return processed;
    }

    // =================================================================
    // 内部工具
    // =================================================================

    private void releaseAll(Long proposalId, Long actor, String reason) {
        Map<String, Object> p = proposalRow(proposalId);
        for (Long itemId : proposalItemIds(proposalId)) items.releaseSwapOccupation(itemId, proposalId, actor, reason);
        Object bundleId = p.get("proposer_bundle_id");
        if (bundleId != null) {
            jdbc.update("UPDATE swap_bundles SET status='PACKED', version=version+1, updated_by=? WHERE id=? AND status='IN_SWAP'",
                    actor, bundleId);
        }
    }

    private List<Long> proposalItemIds(Long proposalId) {
        return jdbc.queryForList("SELECT item_id FROM swap_proposal_items WHERE swap_proposal_id=? ORDER BY id", Long.class, proposalId);
    }

    private SwapBundleDetail bundleDetail(Long bundleId, Long viewerId) {
        Map<String, Object> b = bundleRow(bundleId);
        Long ownerId = ((Number) b.get("owner_id")).longValue();
        if (!ownerId.equals(viewerId) && !isAdmin()) throw new DomainException(ErrorCode.FORBIDDEN);
        if (b.get("deleted_at") != null && !isAdmin()) throw new DomainException(ErrorCode.SWAP_BUNDLE_NOT_FOUND);

        SwapBundleDetail d = new SwapBundleDetail();
        d.setId(bundleId);
        d.setTitle((String) b.get("title"));
        d.setStatus(SwapBundleStatus.valueOf((String) b.get("status")));
        d.setEstimatedTotal(DecimalString.of((BigDecimal) b.get("estimated_total")));
        d.setPriceDisclaimer("用户自填估价，仅供参考，非平台定价");
        d.setVersion(((Number) b.get("version")).intValue());
        d.setCreatedAt(instant(b.get("created_at")));
        d.setUpdatedAt(instant(b.get("updated_at")));

        UserProfile owner = new UserProfile();
        owner.setId(ownerId);
        owner.setNickname((String) b.get("owner_nickname"));
        owner.setAvatarUrl((String) b.get("owner_avatar"));
        owner.setCreatedAt(instant(b.get("owner_created")));
        d.setOwner(owner);

        TradeZone z = new TradeZone();
        z.setId(((Number) b.get("trade_zone_id")).longValue());
        z.setCode((String) b.get("zone_code"));
        z.setName((String) b.get("zone_name"));
        d.setTradeZone(z);

        List<SwapBundleItem> items = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT sbi.*, i.title, i.estimated_price, i.status::text item_status_now, i.category_id, i.condition_id, i.trade_zone_id, "
                + "i.swap_preference, i.published_at, i.seller_id, c.name category_name, ic.name condition_name, tz.name zone_name, "
                + "su.nickname seller_nickname, (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY sort_order,id LIMIT 1) cover "
                + "FROM swap_bundle_items sbi JOIN items i ON i.id=sbi.item_id "
                + "JOIN categories c ON c.id=i.category_id JOIN item_conditions ic ON ic.id=i.condition_id "
                + "JOIN trade_zones tz ON tz.id=i.trade_zone_id JOIN users su ON su.id=i.seller_id "
                + "WHERE sbi.swap_bundle_id=? AND sbi.item_status='ACTIVE' ORDER BY sbi.id", bundleId)) {
            SwapBundleItem bi = new SwapBundleItem();
            bi.setId(((Number) row.get("id")).longValue());
            bi.setItemId(((Number) row.get("item_id")).longValue());
            bi.setItemStatus(BundleItemStatus.valueOf((String) row.get("item_status")));
            bi.setPriceSnapshot(DecimalString.of((BigDecimal) row.get("price_snapshot")));
            bi.setReleasedAt(instant(row.get("released_at")));
            bi.setReleaseReason((String) row.get("release_reason"));
            bi.setItem(summaryFromRow(row));
            items.add(bi);
        }
        d.setItems(items);
        String status = (String) b.get("status");
        d.setEditable("DRAFT".equals(status) || "PACKED".equals(status) || "RELEASED".equals(status));
        return d;
    }

    private SwapBundleSummary bundleSummary(ResultSet rs) throws SQLException {
        SwapBundleSummary s = new SwapBundleSummary();
        s.setId(rs.getLong("id"));
        s.setTitle(rs.getString("title"));
        s.setStatus(SwapBundleStatus.valueOf(rs.getString("status")));
        s.setEstimatedTotal(DecimalString.of(rs.getBigDecimal("estimated_total")));
        s.setTradeZoneId(rs.getLong("trade_zone_id"));
        s.setPackedAt(instant(rs.getTimestamp("packed_at")));
        s.setCreatedAt(instant(rs.getTimestamp("created_at")));
        s.setItemCount(jdbc.queryForObject("SELECT count(*) FROM swap_bundle_items WHERE swap_bundle_id=? AND item_status='ACTIVE'",
                Integer.class, rs.getLong("id")));
        return s;
    }

    private SwapProposalDetail proposalDetail(Long proposalId, Long viewerId) {
        Map<String, Object> p = proposalRow(proposalId);
        Long proposerId = ((Number) p.get("proposer_id")).longValue();
        Long targetId = ((Number) p.get("target_id")).longValue();
        boolean proposer = proposerId.equals(viewerId);
        boolean target = targetId.equals(viewerId);
        if (!proposer && !target && !isAdmin()) throw new DomainException(ErrorCode.FORBIDDEN);

        SwapProposalDetail d = new SwapProposalDetail();
        d.setId(proposalId);
        d.setProposalNo((String) p.get("proposal_no"));
        String status = (String) p.get("status");
        d.setStatus(SwapProposalStatus.valueOf(status));
        d.setRole(proposer ? "PROPOSER" : "TARGET");
        d.setMessage((String) p.get("message"));
        d.setCounterDepth(((Number) p.get("counter_depth")).intValue());
        if (p.get("parent_proposal_id") != null) d.setParentProposalId(((Number) p.get("parent_proposal_id")).longValue());
        d.setOfferTotal(DecimalString.of((BigDecimal) p.get("estimated_total_offer")));
        d.setWantTotal(DecimalString.of((BigDecimal) p.get("estimated_total_want")));
        d.setPriceDisclaimer("用户自填估价，仅供参考，非平台定价");
        d.setVersion(((Number) p.get("version")).intValue());
        d.setCreatedAt(instant(p.get("created_at")));
        d.setUpdatedAt(instant(p.get("updated_at")));
        d.setAcceptedAt(instant(p.get("accepted_at")));
        d.setCompletedAt(instant(p.get("completed_at")));
        d.setProposerConfirmedAt(instant(p.get("proposer_confirmed_at")));
        d.setTargetConfirmedAt(instant(p.get("target_confirmed_at")));
        d.setMyConfirmed(proposer ? p.get("proposer_confirmed_at") != null : p.get("target_confirmed_at") != null);
        if (p.get("termination_reason") != null) {
            d.setTerminationReason(SwapTerminationReason.valueOf((String) p.get("termination_reason")));
        }

        UserProfile proposerProfile = new UserProfile();
        proposerProfile.setId(proposerId);
        proposerProfile.setNickname((String) p.get("proposer_nickname"));
        proposerProfile.setAvatarUrl((String) p.get("proposer_avatar"));
        proposerProfile.setCreatedAt(instant(p.get("proposer_created")));
        d.setProposer(proposerProfile);

        UserProfile targetProfile = new UserProfile();
        targetProfile.setId(targetId);
        targetProfile.setNickname((String) p.get("target_nickname"));
        targetProfile.setAvatarUrl((String) p.get("target_avatar"));
        targetProfile.setCreatedAt(instant(p.get("target_created")));
        d.setTarget(targetProfile);

        TradeZone z = new TradeZone();
        z.setId(((Number) p.get("trade_zone_id")).longValue());
        z.setCode((String) p.get("zone_code"));
        z.setName((String) p.get("zone_name"));
        d.setTradeZone(z);

        if (p.get("proposer_bundle_id") != null) {
            d.setOfferBundle(bundleDetail(((Number) p.get("proposer_bundle_id")).longValue(), proposerId.equals(viewerId) ? viewerId : proposerId));
        }

        List<SwapProposalItem> wants = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT spi.*, i.title, i.estimated_price, i.status::text item_status_now, i.category_id, i.condition_id, i.trade_zone_id, "
                + "i.swap_preference, i.published_at, i.seller_id, c.name category_name, ic.name condition_name, tz.name zone_name, "
                + "su.nickname seller_nickname, (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY sort_order,id LIMIT 1) cover "
                + "FROM swap_proposal_items spi JOIN items i ON i.id=spi.item_id "
                + "JOIN categories c ON c.id=i.category_id JOIN item_conditions ic ON ic.id=i.condition_id "
                + "JOIN trade_zones tz ON tz.id=i.trade_zone_id JOIN users su ON su.id=i.seller_id "
                + "WHERE spi.swap_proposal_id=? AND i.seller_id=? ORDER BY spi.id", proposalId, targetId)) {
            SwapProposalItem wi = new SwapProposalItem();
            wi.setId(((Number) row.get("id")).longValue());
            wi.setItemId(((Number) row.get("item_id")).longValue());
            wi.setTitleSnapshot((String) row.get("title_snapshot"));
            wi.setPriceSnapshot(DecimalString.of((BigDecimal) row.get("price_snapshot")));
            wi.setItem(summaryFromRow(row));
            wants.add(wi);
        }
        d.setWantItems(wants);

        Timestamp expires = (Timestamp) p.get("expires_at");
        if (expires != null) {
            d.setExpiresAt(expires.toInstant());
            d.setRemainingSeconds(Math.max(0, Duration.between(Instant.now(), expires.toInstant()).getSeconds()));
        }

        boolean pending = "PENDING".equals(status);
        d.setCanAccept(target && pending);
        d.setCanReject(target && pending);
        d.setCanCounter(target && pending && ((Number) p.get("counter_depth")).intValue() == 0);
        d.setCanCancel(proposer && pending);
        if (("ACCEPTED".equals(status) || "COMPLETED".equals(status))) {
            Map<String, Object> c = codeMap(proposalId);
            if (c != null && (proposer || target)) {
                SwapCode code = new SwapCode();
                code.setId(((Number) c.get("id")).longValue());
                code.setCode((String) c.get("code"));
                code.setStatus(SwapCodeStatus.valueOf((String) c.get("status")));
                code.setVerifiedAt(instant(c.get("verified_at")));
                if (c.get("verified_by") != null) code.setVerifiedBy(((Number) c.get("verified_by")).longValue());
                code.setExpiresAt(instant(c.get("expires_at")));
                code.setNotice("交换码仅供双方线下当面核对，核销后立即失效；请勿泄露给第三方。");
                d.setSwapCode(code);
            }
        }
        return d;
    }

    private SwapProposalSummary proposalSummary(ResultSet rs, Long viewerId) throws SQLException {
        SwapProposalSummary s = new SwapProposalSummary();
        Long id = rs.getLong("id");
        Long proposerId = rs.getLong("proposer_id");
        Long targetId = rs.getLong("target_id");
        s.setId(id);
        s.setProposalNo(rs.getString("proposal_no"));
        s.setStatus(SwapProposalStatus.valueOf(rs.getString("status")));
        s.setRole(proposerId.equals(viewerId) ? "PROPOSER" : "TARGET");
        s.setOfferTotal(DecimalString.of(rs.getBigDecimal("estimated_total_offer")));
        s.setWantTotal(DecimalString.of(rs.getBigDecimal("estimated_total_want")));
        s.setCounterDepth(rs.getInt("counter_depth"));
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) {
            s.setExpiresAt(expires.toInstant());
            if ("PENDING".equals(rs.getString("status"))) {
                s.setRemainingSeconds(Math.max(0, Duration.between(Instant.now(), expires.toInstant()).getSeconds()));
            }
        }
        s.setCreatedAt(instant(rs.getTimestamp("created_at")));

        UserProfile other = new UserProfile();
        if (proposerId.equals(viewerId)) {
            other.setId(targetId);
            other.setNickname(rs.getString("target_nickname"));
            other.setAvatarUrl(rs.getString("target_avatar"));
            other.setCreatedAt(instant(rs.getTimestamp("target_created")));
        } else {
            other.setId(proposerId);
            other.setNickname(rs.getString("proposer_nickname"));
            other.setAvatarUrl(rs.getString("proposer_avatar"));
            other.setCreatedAt(instant(rs.getTimestamp("proposer_created")));
        }
        s.setCounterparty(other);

        s.setOfferItemCount(jdbc.queryForObject(
                "SELECT count(*) FROM swap_proposal_items spi JOIN items i ON i.id=spi.item_id WHERE spi.swap_proposal_id=? AND i.seller_id=?",
                Integer.class, id, proposerId));
        s.setWantItemCount(jdbc.queryForObject(
                "SELECT count(*) FROM swap_proposal_items spi JOIN items i ON i.id=spi.item_id WHERE spi.swap_proposal_id=? AND i.seller_id=?",
                Integer.class, id, targetId));
        return s;
    }

    private SwapCode codeRow(Long proposalId, Long uid) {
        Map<String, Object> c = codeMap(proposalId);
        if (c == null) throw new DomainException(ErrorCode.SWAP_CODE_NOT_ISSUED);
        SwapCode code = new SwapCode();
        code.setId(((Number) c.get("id")).longValue());
        code.setCode((String) c.get("code"));
        code.setStatus(SwapCodeStatus.valueOf((String) c.get("status")));
        code.setVerifiedAt(instant(c.get("verified_at")));
        if (c.get("verified_by") != null) code.setVerifiedBy(((Number) c.get("verified_by")).longValue());
        code.setExpiresAt(instant(c.get("expires_at")));
        code.setNotice("交换码仅供双方线下当面核对，核销后立即失效；请勿泄露给第三方。");
        return code;
    }

    private Map<String, Object> codeMap(Long proposalId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM swap_codes WHERE swap_proposal_id=?", proposalId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Review reviewRow(Long reviewId) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT rv.*, ru.nickname reviewer_nickname, ru.avatar_url reviewer_avatar, ru.created_at reviewer_created, "
                + "eu.nickname reviewee_nickname, eu.avatar_url reviewee_avatar, eu.created_at reviewee_created "
                + "FROM reviews rv JOIN users ru ON ru.id=rv.reviewer_id JOIN users eu ON eu.id=rv.reviewee_id WHERE rv.id=?", reviewId);
        Review r = new Review();
        r.setId(reviewId);
        if (row.get("order_id") != null) r.setOrderId(((Number) row.get("order_id")).longValue());
        if (row.get("swap_proposal_id") != null) r.setSwapProposalId(((Number) row.get("swap_proposal_id")).longValue());
        r.setRating(((Number) row.get("rating")).intValue());
        r.setContent((String) row.get("content"));
        if (row.get("reviewer_role") != null) r.setReviewerRole(ReviewRole.valueOf((String) row.get("reviewer_role")));
        r.setCreatedAt(instant(row.get("created_at")));

        UserProfile reviewer = new UserProfile();
        reviewer.setId(((Number) row.get("reviewer_id")).longValue());
        reviewer.setNickname((String) row.get("reviewer_nickname"));
        reviewer.setAvatarUrl((String) row.get("reviewer_avatar"));
        reviewer.setCreatedAt(instant(row.get("reviewer_created")));
        r.setReviewer(reviewer);

        UserProfile reviewee = new UserProfile();
        reviewee.setId(((Number) row.get("reviewee_id")).longValue());
        reviewee.setNickname((String) row.get("reviewee_nickname"));
        reviewee.setAvatarUrl((String) row.get("reviewee_avatar"));
        reviewee.setCreatedAt(instant(row.get("reviewee_created")));
        r.setReviewee(reviewee);
        return r;
    }
    // -------- row helpers --------

    private Map<String, Object> itemRow(Long itemId) {
        try {
            return jdbc.queryForMap("SELECT id, seller_id, title, estimated_price, status::text status, trade_zone_id, "
                    + "swap_preference::text swap_preference, deleted_at FROM items WHERE id=?", itemId);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new DomainException(ErrorCode.ITEM_NOT_FOUND);
        }
    }

    private void requireInSale(Map<String, Object> it) {
        String st = (String) it.get("status");
        if ("IN_SALE".equals(st)) return;
        if ("RESERVED".equals(st)) throw new DomainException(ErrorCode.ITEM_ALREADY_RESERVED);
        if ("SWAPPING".equals(st)) throw new DomainException(ErrorCode.SWAP_ITEM_OCCUPIED);
        throw new DomainException(ErrorCode.ITEM_NOT_PURCHASABLE);
    }

    private Map<String, Object> bundleRow(Long bundleId) {
        List<Map<String, Object>> rows = jdbc.queryForList(BUNDLE_SELECT + "WHERE b.id=?", bundleId);
        if (rows.isEmpty()) throw new DomainException(ErrorCode.SWAP_BUNDLE_NOT_FOUND);
        return rows.get(0);
    }

    private Map<String, Object> ownedBundle(Long bundleId, Long uid) {
        Map<String, Object> b = bundleRow(bundleId);
        if (((Number) b.get("owner_id")).longValue() != uid) throw new DomainException(ErrorCode.FORBIDDEN);
        if (b.get("deleted_at") != null) throw new DomainException(ErrorCode.SWAP_BUNDLE_NOT_FOUND);
        return b;
    }

    private Map<String, Object> proposalRow(Long proposalId) {
        List<Map<String, Object>> rows = jdbc.queryForList(PROPOSAL_SELECT + "WHERE p.id=?", proposalId);
        if (rows.isEmpty()) throw new DomainException(ErrorCode.SWAP_PROPOSAL_NOT_FOUND);
        return rows.get(0);
    }

    private void requireParticipant(Map<String, Object> p, Long uid) {
        boolean participant = ((Number) p.get("proposer_id")).longValue() == uid.longValue()
                || ((Number) p.get("target_id")).longValue() == uid.longValue();
        if (!participant && !isAdmin()) throw new DomainException(ErrorCode.FORBIDDEN);
    }

    private ItemSummary summaryFromRow(Map<String, Object> row) {
        ItemSummary s = new ItemSummary();
        s.setId(((Number) row.get("item_id")).longValue());
        s.setTitle((String) row.get("title"));
        s.setCoverImageUrl((String) row.get("cover"));
        s.setEstimatedPrice(DecimalString.of((BigDecimal) row.get("estimated_price")));
        s.setCurrency("CNY");
        s.setStatus(ItemStatus.valueOf((String) row.get("item_status_now")));
        s.setSwapPreference(SwapPreference.valueOf((String) row.get("swap_preference")));
        s.setCategoryId(((Number) row.get("category_id")).longValue());
        s.setCategoryName((String) row.get("category_name"));
        s.setConditionId(((Number) row.get("condition_id")).longValue());
        s.setConditionName((String) row.get("condition_name"));
        s.setTradeZoneId(((Number) row.get("trade_zone_id")).longValue());
        s.setTradeZoneName((String) row.get("zone_name"));
        s.setSellerId(((Number) row.get("seller_id")).longValue());
        s.setSellerNickname((String) row.get("seller_nickname"));
        s.setPublishedAt(instant(row.get("published_at")));
        return s;
    }

    private void ensureZoneSelected(Long uid, Long zoneId) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM user_trade_zones WHERE user_id=? AND trade_zone_id=?", Long.class, uid, zoneId);
        if (n == null || n == 0) throw new DomainException(ErrorCode.TRADE_ZONE_NOT_SELECTED);
    }

    private void ensureNotRestricted(Long uid) {
        String status = jdbc.queryForObject("SELECT status::text FROM users WHERE id=?", String.class, uid);
        if ("RESTRICTED".equals(status)) throw new DomainException(ErrorCode.USER_RESTRICTED_CANNOT_WRITE);
    }

    private List<Long> distinct(List<Long> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private String nextNo() {
        return "SW" + NO_FMT.format(Instant.now()) + ThreadLocalRandom.current().nextInt(100000, 999999);
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
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant();
        return null;
    }
}
