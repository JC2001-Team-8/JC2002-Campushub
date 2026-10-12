package com.jc2001.campustrade.service;

import com.jc2001.campustrade.dto.response.Category;
import com.jc2001.campustrade.dto.response.ItemCondition;
import com.jc2001.campustrade.dto.response.PickupPoint;
import com.jc2001.campustrade.dto.response.PickupPointType;
import com.jc2001.campustrade.dto.response.TradeZone;
import com.jc2001.campustrade.dto.response.ZoneType;
import java.util.List;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * DictionaryService（Service 接口骨架）。
 *
 * <p><b>本步骤不实现业务逻辑</b>：实现类统一抛 UnsupportedOperationException。
 *
 * <p><b>返回原始领域类型</b>（如 OrderDetail / PageResult&lt;ItemSummary&gt;），
 * 由 Controller 负责包成 ApiResponse —— 包络属接口层职责，不下沉到 Service。
 *
 * <p><b>下一步实现必须遵守</b>（见 idempotency-and-concurrency.md）：
 * <ul>
 *   <li>状态转换使用条件更新或乐观锁，<b>禁止「先查后改」</b>（R-18）</li>
 *   <li>占用/释放必须与 items.status 投影<b>同一事务</b>（R-16 / DD-01）</li>
 *   <li>部分唯一索引冲突（SQLState 23505）映射为对应域 409，而非 500</li>
 *   <li>归属校验失败返回 403，不返回 404（E-02）</li>
 * </ul>
 */
public interface DictionaryService {

    /**
     * 预设交易区域列表（R-15）
     *
     * <p>对应 operationId: <code>listTradeZones</code>
     * <p>TODO 下一步：实现
     */
    List<TradeZone> listTradeZones(ZoneType zoneType, Boolean includeInactive);

    /**
     * 按交易区域查询公共取货点（R-06）
     *
     * <p>对应 operationId: <code>listPickupPointsByZone</code>
     * <p>TODO 下一步：实现
     */
    List<PickupPoint> listPickupPointsByZone(Long tradeZoneId, PickupPointType pointType);

    /**
     * 商品分类字典（FR-206）
     *
     * <p>对应 operationId: <code>listCategories</code>
     * <p>TODO 下一步：实现
     */
    List<Category> listCategories();

    /**
     * 商品成色字典（FR-207）
     *
     * <p>对应 operationId: <code>listItemConditions</code>
     * <p>TODO 下一步：实现
     */
    List<ItemCondition> listItemConditions();

}
