package com.jc2001.campustrade.common;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码枚举（骨架阶段）。
 *
 * <p><b>命名规范</b>（见 {@code docs/iteration1/design/error-codes.md} §1）：
 * 格式为 {@code DOMAIN_REASON}，域前缀含 AUTH_ / USER_ / ITEM_ / CART_ / ORDER_ /
 * SWAP_ / REVIEW_ / REPORT_ / GOVERNANCE_ / APPEAL_ / FILE_ / VALIDATION_ / COMMON_。
 * 错误码是**机器可判定**的标识，前端必须依据 code 分支，不得依赖 message 文本。
 *
 * <p><b>本步骤范围（重要）</b>：按任务要求只实现 7 个**通用**错误码。
 * 契约中已冻结的 93 个错误码（{@code error-codes.md}）在下一步按域补齐，
 * 补齐前不得自行发明新码名——必须与 {@code error-codes.md} 逐字一致。
 *
 * <p><b>HTTP 语义分区</b>（与 {@code api-contract.md} §5.1 对齐）：
 * <ul>
 *   <li>400 语法/格式错误、401 未认证、403 已认证但无权限</li>
 *   <li>404 资源不存在（<b>不用于掩盖越权</b>，越权返回 403）</li>
 *   <li>409 状态/并发冲突、422 业务规则不满足、429 限流、500 服务端错误</li>
 * </ul>
 *
 * @see GlobalExceptionHandler
 */
public enum ErrorCode {

    // =================================================================
    // 通用（本步骤实现的 7 个）
    //   code 值与 docs/iteration1/design/error-codes.md §3.1 保持一致
    // =================================================================

    /** 成功。契约规定成功响应的 code 固定为 {@code OK}（api-contract.md AD-01）。 */
    OK("OK", HttpStatus.OK, "成功"),

    /** 500：服务端未预期异常。响应携带 traceId 便于日志定位。 */
    INTERNAL_ERROR("COMMON_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请稍后重试"),

    /** 400：参数校验失败（字段级错误见 details）。 */
    VALIDATION_ERROR("VALIDATION_INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "参数校验失败"),

    /** 401：未登录或登录态失效（E-01）。 */
    UNAUTHORIZED("AUTH_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "未登录或登录态已失效，请重新登录"),

    /** 403：已认证但无权限（E-02 非本人 / E-03 被处罚 / 非管理员）。 */
    FORBIDDEN("AUTH_NOT_RESOURCE_OWNER", HttpStatus.FORBIDDEN, "无权执行该操作"),

    /** 404：资源不存在。**仅用于路由目标不存在**，越权不使用 404。 */
    NOT_FOUND("COMMON_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "请求的资源不存在"),

    /** 409：状态或并发冲突（占用冲突 / 终态保护 / 幂等冲突 / 乐观锁）。 */
    CONFLICT("COMMON_VERSION_CONFLICT", HttpStatus.CONFLICT, "数据已被其他请求修改，请刷新后重试"),

    // 账户与共享内核（均逐字对应 docs/iteration1/design/error-codes.md）
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),
    AUTH_USERNAME_TAKEN("AUTH_USERNAME_TAKEN", HttpStatus.CONFLICT, "用户名或邮箱已被占用"),
    COMMON_NOT_IMPLEMENTED("COMMON_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED, "此功能暂未启用"),
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "用户不存在"),
    USER_TRADE_ZONE_REQUIRED("USER_TRADE_ZONE_REQUIRED", HttpStatus.UNPROCESSABLE_ENTITY, "请先设置常用交易区域"),
    TRADE_ZONE_NOT_FOUND("TRADE_ZONE_NOT_FOUND", HttpStatus.NOT_FOUND, "交易区域不存在或已停用"),
    PICKUP_POINT_NOT_FOUND("PICKUP_POINT_NOT_FOUND", HttpStatus.NOT_FOUND, "取货点不存在"),
    PICKUP_POINT_ZONE_MISMATCH("PICKUP_POINT_ZONE_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "取货点不属于指定交易区域"),
    USER_RESTRICTED_CANNOT_WRITE("USER_RESTRICTED_CANNOT_WRITE", HttpStatus.FORBIDDEN, "处罚期间不能执行此操作"),
    COMMON_IDEMPOTENCY_KEY_REUSED("COMMON_IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT, "同一幂等键不能用于不同请求"),
    ITEM_NOT_FOUND("ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "商品不存在"),
    ITEM_LOCKED_BY_OCCUPATION("ITEM_LOCKED_BY_OCCUPATION", HttpStatus.CONFLICT, "商品正在交易中，无法修改"),
    ITEM_STATUS_NOT_EDITABLE("ITEM_STATUS_NOT_EDITABLE", HttpStatus.CONFLICT, "当前商品状态不可编辑"),
    ITEM_BANNED_CANNOT_RELIST("ITEM_BANNED_CANNOT_RELIST", HttpStatus.CONFLICT, "违规下架商品不能自行重新上架"),
    ITEM_STATUS_NOT_SWAPPABLE("ITEM_STATUS_NOT_SWAPPABLE", HttpStatus.CONFLICT, "当前商品状态不能设置交换意愿"),
    TRADE_ZONE_NOT_SELECTED("TRADE_ZONE_NOT_SELECTED", HttpStatus.UNPROCESSABLE_ENTITY, "请先将该交易区域设为常用区域"),
    ITEM_TRADE_ZONE_NOT_SELECTED("ITEM_TRADE_ZONE_NOT_SELECTED", HttpStatus.UNPROCESSABLE_ENTITY, "所选交易区域不在已勾选范围内"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", HttpStatus.UNPROCESSABLE_ENTITY, "图片不能超过 5 MB"),
    FILE_TYPE_NOT_ALLOWED("FILE_TYPE_NOT_ALLOWED", HttpStatus.UNPROCESSABLE_ENTITY, "仅支持 JPG、PNG 或 WebP 图片"),
    COMMON_INVALID_SORT_FIELD("COMMON_INVALID_SORT_FIELD", HttpStatus.BAD_REQUEST, "不支持该排序字段"),

    // =================================================================
    // 购物车（slice 3A）
    // =================================================================
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "购物车条目不存在"),
    CART_OWN_ITEM_NOT_ALLOWED("CART_OWN_ITEM_NOT_ALLOWED", HttpStatus.UNPROCESSABLE_ENTITY, "不能加入自己发布的商品"),
    CART_ITEM_NOT_PURCHASABLE("CART_ITEM_NOT_PURCHASABLE", HttpStatus.UNPROCESSABLE_ENTITY, "该条目当前不可结算"),

    // =================================================================
    // 商品占用冲突（slice 3A）
    // =================================================================
    ITEM_ALREADY_RESERVED("ITEM_ALREADY_RESERVED", HttpStatus.CONFLICT, "商品已被其他订单占用"),
    ITEM_RESERVED_BY_SWAP("ITEM_RESERVED_BY_SWAP", HttpStatus.CONFLICT, "商品正在交换中"),
    ITEM_NOT_PURCHASABLE("ITEM_NOT_PURCHASABLE", HttpStatus.UNPROCESSABLE_ENTITY, "商品当前不可购买"),
    // =================================================================
    // 订单（slice 3A）
    // =================================================================
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "订单不存在"),
    ORDER_TIMEOUT_RELEASED("ORDER_TIMEOUT_RELEASED", HttpStatus.CONFLICT, "订单已超时取消，商品已释放"),
    ORDER_ALREADY_CANCELLED("ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT, "订单已取消"),
    ORDER_STATUS_NOT_PAYABLE("ORDER_STATUS_NOT_PAYABLE", HttpStatus.CONFLICT, "当前状态不可付款"),
    ORDER_CANNOT_CANCEL_AFTER_PAID("ORDER_CANNOT_CANCEL_AFTER_PAID", HttpStatus.CONFLICT, "已付款订单不可自助取消"),
    ORDER_STATUS_NOT_DELIVERABLE("ORDER_STATUS_NOT_DELIVERABLE", HttpStatus.CONFLICT, "当前状态不可标记交付"),
    ORDER_PICKUP_POINT_NOT_SELECTED("ORDER_PICKUP_POINT_NOT_SELECTED", HttpStatus.CONFLICT, "尚未选择取货点"),
    ORDER_NOT_PAID_FOR_PICKUP_SELECTION("ORDER_NOT_PAID_FOR_PICKUP_SELECTION", HttpStatus.CONFLICT, "未付款不可选择取货点"),
    ORDER_ALREADY_DELIVERED("ORDER_ALREADY_DELIVERED", HttpStatus.CONFLICT, "订单已交付，不可修改取货点"),
    ORDER_STATUS_NOT_DELIVERED("ORDER_STATUS_NOT_DELIVERED", HttpStatus.CONFLICT, "订单未交付，不可确认收货"),
    ORDER_ITEM_SELLER_MISMATCH("ORDER_ITEM_SELLER_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "订单明细跨卖家");

    // =================================================================
    // TODO（下一步按 error-codes.md 补齐，不得自行命名）
    //   - 认证与授权：AUTH_INVALID_CREDENTIALS / AUTH_TOKEN_EXPIRED /
    //     AUTH_USERNAME_TAKEN / AUTH_ADMIN_REQUIRED / AUTH_FORBIDDEN_TRADING_AS_ADMIN
    //   - 用户与处罚：USER_RESTRICTED_CANNOT_WRITE / USER_NOT_FOUND / USER_TRADE_ZONE_REQUIRED
    //   - 商品：ITEM_NOT_FOUND / ITEM_ALREADY_RESERVED / ITEM_RESERVED_BY_SWAP /
    //     ITEM_LOCKED_BY_OCCUPATION / ITEM_BANNED_CANNOT_RELIST / ITEM_STATUS_NOT_SWAPPABLE
    //   - 区域与取货点：TRADE_ZONE_MISMATCH(E-17) / TRADE_ZONE_NOT_FOUND /
    //     PICKUP_POINT_NOT_FOUND / PICKUP_POINT_ZONE_MISMATCH(R-06)
    //   - 购物车：CART_OWN_ITEM_NOT_ALLOWED / CART_ITEM_NOT_PURCHASABLE
    //   - 订单：ORDER_TIMEOUT_RELEASED(E-05) / ORDER_CANNOT_CANCEL_AFTER_PAID(Q-08) /
    //     ORDER_PICKUP_POINT_NOT_SELECTED(R-06) / ORDER_STATUS_NOT_DELIVERED
    //   - 交换：SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE(E-07) / SWAP_ITEM_OCCUPIED(E-10) /
    //     SWAP_TARGET_NOT_ACCEPTING(E-08) / SWAP_PROPOSAL_ALREADY_ACCEPTED(E-09) /
    //     SWAP_COUNTER_NOT_ALLOWED(R-28) / SWAP_PARTIAL_ACCEPT_NOT_SUPPORTED(OOS-04) /
    //     SWAP_CODE_ALREADY_VERIFIED(E-12) / SWAP_PROPOSAL_VOIDED(E-11 / R-24)
    //   - 评价/举报/治理/申诉/文件：见 error-codes.md §3.10 ~ §3.13
    //   - 幂等：COMMON_IDEMPOTENCY_KEY_REUSED(E-16) / COMMON_RATE_LIMITED(429)
    // =================================================================

    /** 机器可判定标识（前端按此分支）。 */
    private final String code;

    /** 对应 HTTP 状态码。 */
    private final HttpStatus httpStatus;

    /** 面向用户的中文提示（文案可优化，语义不得变）。 */
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
