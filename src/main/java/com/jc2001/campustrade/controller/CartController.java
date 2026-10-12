package com.jc2001.campustrade.controller;

import com.jc2001.campustrade.common.ApiResponse;
import com.jc2001.campustrade.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jc2001.campustrade.dto.request.AddCartItemRequest;
import com.jc2001.campustrade.dto.request.UpdateCartItemRequest;
import com.jc2001.campustrade.dto.response.CartItem;
import com.jc2001.campustrade.dto.response.CartView;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 购物车（FR-301 ~ FR-302）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "05-购物车", description = "购物车（FR-301 ~ FR-302）")
@RestController
@RequestMapping("/api/v1")
public class CartController {

    /**
     * 查看购物车（按卖家分组）（FR-302）
     *
     * <p>operationId: <code>getCart</code>
     * <p>权限: USER, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看购物车（按卖家分组）（FR-302）", operationId = "getCart", description = "**必须本人**。返回**按卖家分组**的购物车条目（分组即提交订单时的拆单依据，R-05 / C-02）。  - 失效条目（商品被占用/售出/下架）以 `purchasable=false` + `unavailableReason` 返回，前端灰显。 - 被处罚用户**允许查看**（只读，permission-matrix §8）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<CartView>> getCart() {
        throw new UnsupportedOperationException("TODO: implement getCart");
    }

    /**
     * 加入购物车（FR-301）
     *
     * <p>operationId: <code>addCartItem</code>
     * <p>权限: USER
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "加入购物车（FR-301）", operationId = "addCartItem", description = "**必须登录且未被处罚**。规则（R-04）： - **不可加入自己发布的商品** → `422 CART_OWN_ITEM_NOT_ALLOWED`； - 同一商品**不重复加入**，重复加入**幂等**（IDEM-A，返回既有条目，不产生重复行）； - 商品已被占用/售出/下架 → `409 ITEM_ALREADY_RESERVED` / `422 ITEM_NOT_PURCHASABLE`。  **数量固定为 1**（假设 A-16 / Q-23），请求体无需数量字段。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cart/items")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<CartItem>> addCartItem(
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AddCartItemRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement addCartItem");
    }

    /**
     * 修改购物车条目（permission-matrix 4.3）
     *
     * <p>operationId: <code>updateCartItem</code>
     * <p>权限: USER
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "修改购物车条目（permission-matrix 4.3）", operationId = "updateCartItem", description = "**必须本人**。因数量固定为 1（A-16），本接口**不接受数量变更**； 仅用于刷新条目有效性快照并返回最新可结算状态。 非本人返回 `403 AUTH_NOT_RESOURCE_OWNER`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/cart/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartItem>> updateCartItem(
            @Parameter(description = "") @PathVariable("cartItemId") Long cartItemId,
            @RequestBody UpdateCartItemRequest body
    ) {
        throw new UnsupportedOperationException("TODO: implement updateCartItem");
    }

    /**
     * 移除购物车条目（permission-matrix 4.3）
     *
     * <p>operationId: <code>removeCartItem</code>
     * <p>权限: USER
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "移除购物车条目（permission-matrix 4.3）", operationId = "removeCartItem", description = "**必须本人**。**幂等**：重复删除返回 `204`（不存在也视为成功，避免重复点击报错）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/cart/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @Parameter(description = "") @PathVariable("cartItemId") Long cartItemId
    ) {
        throw new UnsupportedOperationException("TODO: implement removeCartItem");
    }

}
