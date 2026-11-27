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
}
