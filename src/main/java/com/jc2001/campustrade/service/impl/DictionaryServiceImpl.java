package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.DictionaryService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.dto.response.Category;
import com.jc2001.campustrade.dto.response.ItemCondition;
import com.jc2001.campustrade.dto.response.PickupPoint;
import com.jc2001.campustrade.dto.response.PickupPointType;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.ZoneType;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * DictionaryService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class DictionaryServiceImpl implements DictionaryService {

    /**
     * 预设交易区域列表（R-15）（未实现）
     *
     * <p>operationId: <code>listTradeZones</code>
     */
    @Override
    public List<TradeZone> listTradeZones(ZoneType zoneType, Boolean includeInactive) {
        throw new UnsupportedOperationException(
                "TODO: implement listTradeZones");
    }

    /**
     * 按交易区域查询公共取货点（R-06）（未实现）
     *
     * <p>operationId: <code>listPickupPointsByZone</code>
     */
    @Override
    public List<PickupPoint> listPickupPointsByZone(Long tradeZoneId, PickupPointType pointType) {
        throw new UnsupportedOperationException(
                "TODO: implement listPickupPointsByZone");
    }

    /**
     * 商品分类字典（FR-206）（未实现）
     *
     * <p>operationId: <code>listCategories</code>
     */
    @Override
    public List<Category> listCategories() {
        throw new UnsupportedOperationException(
                "TODO: implement listCategories");
    }

    /**
     * 商品成色字典（FR-207）（未实现）
     *
     * <p>operationId: <code>listItemConditions</code>
     */
    @Override
    public List<ItemCondition> listItemConditions() {
        throw new UnsupportedOperationException(
                "TODO: implement listItemConditions");
    }

}
