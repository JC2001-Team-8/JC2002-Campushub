package com.jc2001.campustrade.service;

import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateItemRequest;
import com.jc2001.campustrade.dto.request.UpdateItemRequest;
import com.jc2001.campustrade.dto.request.UpdateSwapPreferenceRequest;
import com.jc2001.campustrade.dto.response.*;
import java.util.List;
import java.util.Map;

public interface ItemService {
    PageResult<ItemSummary> list(Integer page, Integer size, List<String> sort, Long categoryId, Long conditionId,
            Long zoneId, String keyword, DecimalString min, DecimalString max, SwapPreference preference, Long sellerId,
            Long currentUser, List<ItemStatus> statuses);
    ItemDetail create(Long userId, String key, CreateItemRequest request);
    ItemDetail detail(Long id, Long viewerId);
    ItemDetail update(Long userId, Long id, UpdateItemRequest request);
    ItemDetail shelf(Long userId, Long id);
    ItemDetail relist(Long userId, Long id);
    ItemDetail preference(Long userId, Long id, UpdateSwapPreferenceRequest request);
    Map<String,Object> occupation(Long userId, Long id, boolean admin);

    /**
     * 切片 3 依赖：下单占用商品（{@code items.status → RESERVED} + 写 {@code item_occupations}）。
     *
     * <p>必须在调用方事务内执行，保证「占用台账 + 商品状态投影」原子（R-16 / DD-01）。
     * <p>占用失败抛 {@code ITEM_ALREADY_RESERVED} / {@code ITEM_RESERVED_BY_SWAP} / {@code ITEM_NOT_PURCHASABLE}。
     */
    void occupyForOrder(Long itemId, Long orderId, Long actorId);

    /** 切片 3 依赖：释放订单占用（取消/超时）。幂等：无进行中占用时直接返回。 */
    void releaseOrderOccupation(Long itemId, Long orderId, Long actorId, String reason);

    /** 切片 3 依赖：确认收货后把占用态商品置为 {@code SOLD}（幂等）。 */
    void markSold(Long itemId, Long actorId);
}
