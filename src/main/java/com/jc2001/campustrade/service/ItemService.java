package com.jc2001.campustrade.service;

import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateItemRequest;
import com.jc2001.campustrade.dto.request.UpdateItemRequest;
import com.jc2001.campustrade.dto.request.UpdateSwapPreferenceRequest;
import com.jc2001.campustrade.dto.response.DecimalString;
import com.jc2001.campustrade.dto.response.ItemDetail;
import com.jc2001.campustrade.dto.response.ItemImageUploadResponse;
import com.jc2001.campustrade.dto.response.ItemStatus;
import com.jc2001.campustrade.dto.response.ItemSummary;
import com.jc2001.campustrade.dto.response.SwapPreference;
import java.util.List;
import java.util.Map;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * ItemService（Service 接口骨架）。
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
public interface ItemService {

    /**
     * 商品列表 / 分类浏览 / 搜索（FR-201 前置浏览、PF-01）
     *
     * <p>对应 operationId: <code>listItems</code>
     * <p>TODO 下一步：实现
     */
    PageResult<ItemSummary> listItems(
            Integer page,
            Integer size,
            List<String> sort,
            Long categoryId,
            Long conditionId,
            Long tradeZoneId,
            String keyword,
            DecimalString minPrice,
            DecimalString maxPrice,
            SwapPreference swapPreference,
            Long sellerId
    );

    /**
     * 发布商品（FR-201）
     *
     * <p>对应 operationId: <code>createItem</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail createItem(String idempotencyKey, CreateItemRequest body);

    /**
     * 我的物品（FR-202 前置、PF-08）
     *
     * <p>对应 operationId: <code>listMyItems</code>
     * <p>TODO 下一步：实现
     */
    PageResult<ItemSummary> listMyItems(Integer page, Integer size, List<String> sort, List<ItemStatus> status);

    /**
     * 商品详情（FR-210、PF-02）
     *
     * <p>对应 operationId: <code>getItemDetail</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail getItemDetail(Long itemId);

    /**
     * 编辑商品（FR-202）
     *
     * <p>对应 operationId: <code>updateItem</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail updateItem(Long itemId, String idempotencyKey, UpdateItemRequest body);

    /**
     * 下架商品（FR-203）
     *
     * <p>对应 operationId: <code>takeItemOffShelf</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail takeItemOffShelf(Long itemId, String idempotencyKey);

    /**
     * 重新上架（FR-204）
     *
     * <p>对应 operationId: <code>relistItem</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail relistItem(Long itemId, String idempotencyKey);

    /**
     * 设置商品交换意愿（FR-401 / R-20）
     *
     * <p>对应 operationId: <code>updateItemSwapPreference</code>
     * <p>TODO 下一步：实现
     */
    ItemDetail updateItemSwapPreference(Long itemId, String idempotencyKey, UpdateSwapPreferenceRequest body);

    /**
     * 查看商品当前占用情况（permission-matrix 3.11）
     *
     * <p>对应 operationId: <code>getItemOccupation</code>
     * <p>TODO 下一步：实现
     */
    Map<String, Object> getItemOccupation(Long itemId);

    /**
     * 上传图片（FR-205 图片能力）
     *
     * <p>对应 operationId: <code>uploadImage</code>
     * <p>TODO 下一步：实现
     */
    ItemImageUploadResponse uploadImage();

}
