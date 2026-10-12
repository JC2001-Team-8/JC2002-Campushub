package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.CartService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.dto.request.AddCartItemRequest;
import com.jc2001.campustrade.dto.request.UpdateCartItemRequest;
import com.jc2001.campustrade.dto.response.CartItem;
import com.jc2001.campustrade.dto.response.CartView;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * CartService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class CartServiceImpl implements CartService {

    /**
     * 查看购物车（按卖家分组）（FR-302）（未实现）
     *
     * <p>operationId: <code>getCart</code>
     */
    @Override
    public CartView getCart() {
        throw new UnsupportedOperationException(
                "TODO: implement getCart");
    }

    /**
     * 加入购物车（FR-301）（未实现）
     *
     * <p>operationId: <code>addCartItem</code>
     */
    @Override
    public CartItem addCartItem(String idempotencyKey, AddCartItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement addCartItem");
    }

    /**
     * 修改购物车条目（permission-matrix 4.3）（未实现）
     *
     * <p>operationId: <code>updateCartItem</code>
     */
    @Override
    public CartItem updateCartItem(Long cartItemId, UpdateCartItemRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement updateCartItem");
    }

    /**
     * 移除购物车条目（permission-matrix 4.3）（未实现）
     *
     * <p>operationId: <code>removeCartItem</code>
     */
    @Override
    public Void removeCartItem(Long cartItemId) {
        throw new UnsupportedOperationException(
                "TODO: implement removeCartItem");
    }

}
