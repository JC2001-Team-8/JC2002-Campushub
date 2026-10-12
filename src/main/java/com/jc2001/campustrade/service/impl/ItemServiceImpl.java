package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.ItemService;
import org.springframework.stereotype.Service;
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
 * ItemService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class ItemServiceImpl implements ItemService {

    /**
     * 商品列表 / 分类浏览 / 搜索（FR-201 前置浏览、PF-01）（未实现）
     *
     * <p>operationId: <code>listItems</code>
     */
    @Override
    public PageResult<ItemSummary> listItems(Integer page, Integer size, List<String> sort, Long categoryId, Long conditionId, Long tradeZoneId, String keyword, DecimalString minPrice, DecimalString maxPrice, SwapPreference swapPreference, Long sellerId) {
        throw new UnsupportedOperationException(
                "TODO: implement listItems");
    }

    /**
     * 发布商品（FR-201）（未实现）
     *
     * <p>operationId: <code>createItem</code>
     */
    @Override
    public ItemDetail createItem(String idempotencyKey, CreateItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement createItem");
    }

    /**
     * 我的物品（FR-202 前置、PF-08）（未实现）
     *
     * <p>operationId: <code>listMyItems</code>
     */
    @Override
    public PageResult<ItemSummary> listMyItems(Integer page, Integer size, List<String> sort, List<ItemStatus> status) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyItems");
    }

    /**
     * 商品详情（FR-210、PF-02）（未实现）
     *
     * <p>operationId: <code>getItemDetail</code>
     */
    @Override
    public ItemDetail getItemDetail(Long itemId) {
        throw new UnsupportedOperationException(
                "TODO: implement getItemDetail");
    }

    /**
     * 编辑商品（FR-202）（未实现）
     *
     * <p>operationId: <code>updateItem</code>
     */
    @Override
    public ItemDetail updateItem(Long itemId, String idempotencyKey, UpdateItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateItem");
    }

    /**
     * 下架商品（FR-203）（未实现）
     *
     * <p>operationId: <code>takeItemOffShelf</code>
     */
    @Override
    public ItemDetail takeItemOffShelf(Long itemId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement takeItemOffShelf");
    }

    /**
     * 重新上架（FR-204）（未实现）
     *
     * <p>operationId: <code>relistItem</code>
     */
    @Override
    public ItemDetail relistItem(Long itemId, String idempotencyKey) {
        throw new UnsupportedOperationException(
                "TODO: implement relistItem");
    }

    /**
     * 设置商品交换意愿（FR-401 / R-20）（未实现）
     *
     * <p>operationId: <code>updateItemSwapPreference</code>
     */
    @Override
    public ItemDetail updateItemSwapPreference(Long itemId, String idempotencyKey, UpdateSwapPreferenceRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateItemSwapPreference");
    }

    /**
     * 查看商品当前占用情况（permission-matrix 3.11）（未实现）
     *
     * <p>operationId: <code>getItemOccupation</code>
     */
    @Override
    public Map<String, Object> getItemOccupation(Long itemId) {
        throw new UnsupportedOperationException(
                "TODO: implement getItemOccupation");
    }

    /**
     * 上传图片（FR-205 图片能力）（未实现）
     *
     * <p>operationId: <code>uploadImage</code>
     */
    @Override
    public ItemImageUploadResponse uploadImage() {
        throw new UnsupportedOperationException(
                "TODO: implement uploadImage");
    }

}
