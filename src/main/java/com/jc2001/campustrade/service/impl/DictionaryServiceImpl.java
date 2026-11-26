package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.common.DomainException;
import com.jc2001.campustrade.common.ErrorCode;
import com.jc2001.campustrade.dto.response.*;
import com.jc2001.campustrade.repository.CategoryRepository;
import com.jc2001.campustrade.repository.ItemConditionRepository;
import com.jc2001.campustrade.repository.PickupPointRepository;
import com.jc2001.campustrade.repository.TradeZoneRepository;
import com.jc2001.campustrade.service.DictionaryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictionaryServiceImpl implements DictionaryService {
    private final TradeZoneRepository zones;
    private final PickupPointRepository pickupPoints;
    private final CategoryRepository categories;
    private final ItemConditionRepository conditions;
    public DictionaryServiceImpl(TradeZoneRepository zones, PickupPointRepository pickupPoints,
                                 CategoryRepository categories, ItemConditionRepository conditions) {
        this.zones=zones; this.pickupPoints=pickupPoints; this.categories=categories; this.conditions=conditions;
    }

    @Override public List<TradeZone> listTradeZones(ZoneType zoneType, Boolean includeInactive) {
        boolean admin = isAdmin();
        return zones.findForPublic(zoneType == null ? null : zoneType.name(), Boolean.TRUE.equals(includeInactive) && admin);
    }

    @Override public List<PickupPoint> listPickupPointsByZone(Long tradeZoneId, PickupPointType pointType) {
        if (!zones.exists(tradeZoneId)) throw new DomainException(ErrorCode.TRADE_ZONE_NOT_FOUND);
        return pickupPoints.findActiveByZone(tradeZoneId, pointType == null ? null : pointType.name());
    }

    @Override public List<Category> listCategories() { return categories.findActive(); }
    @Override public List<ItemCondition> listItemConditions() { return conditions.findActive(); }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
