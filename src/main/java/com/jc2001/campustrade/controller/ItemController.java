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
import com.jc2001.campustrade.service.ItemService;
import com.jc2001.campustrade.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.*;
import java.util.UUID;
import javax.validation.Valid;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 商品（FR-201 ~ FR-211）
 *
 * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，
 * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；
 * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。
 *
 * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}
 * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}
 * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）
 */
@Tag(name = "04-商品", description = "商品（FR-201 ~ FR-211）")
@RestController
@RequestMapping("/api/v1")
public class ItemController {

    private final ItemService items;
    @Value("${app.storage.local-dir:./data/uploads}") private String uploadDir;
    public ItemController(ItemService items) { this.items = items; }

    /**
     * 商品列表 / 分类浏览 / 搜索（FR-201 前置浏览、PF-01）
     *
     * <p>operationId: <code>listItems</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "商品列表 / 分类浏览 / 搜索（FR-201 前置浏览、PF-01）", operationId = "listItems", description = "**访客可用**（无需登录）。仅返回状态为 `IN_SALE` 的商品； `RESERVED` / `SWAPPING` / `SOLD` / `OFF_SHELF` / `BANNED` 默认不出现在列表中。  支持的筛选（**迭代一固定、可解释，不做推荐算法** OOS-09）： `categoryId`、`conditionId`、`tradeZoneId`、`keyword`、`minPrice`、`maxPrice`、`swapPreference`、`sellerId`。  **明确不支持**：距离筛选、附近排序、个性化推荐、地理坐标参数（GEO-04 / OOS-06 / OOS-09）。 传入未知排序字段返回 `400 COMMON_INVALID_SORT_FIELD`。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResult<ItemSummary>>> listItems(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "排序表达式，格式 `字段,方向`（方向 `asc` | `desc`），可重复传入多级排序。 **迭代一允许的排序字段是白名单**（OOS-09 不做推荐/个性化排序）： 商品列表：`published_at`（默认 desc）、`estimated_price`； 订单列表：`created_at`（默认 desc）； 交换提议：`created_at`（默认 desc）； 通知：`created_at`（默认 desc）。 传入白名单外字段返回 `COMMON_INVALID_SORT_FIELD`。") @RequestParam(value = "sort", required = false) List<String> sort,
            @Parameter(description = "分类过滤（FR-206）") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "成色过滤（FR-207）") @RequestParam(value = "conditionId", required = false) Long conditionId,
            @Parameter(description = "交易区域过滤（R-15）。**迭代一唯一的「地理」维度**； 「区域内商品」通过区域 ID 表达，**不使用经纬度或距离**（OOS-06）。") @RequestParam(value = "tradeZoneId", required = false) Long tradeZoneId,
            @Parameter(description = "关键词（匹配标题，大小写不敏感前缀/包含匹配；不做分词检索）") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "估价下限（含），单位元，2 位小数") @RequestParam(value = "minPrice", required = false) DecimalString minPrice,
            @Parameter(description = "估价上限（含）") @RequestParam(value = "maxPrice", required = false) DecimalString maxPrice,
            @Parameter(description = "是否接受交换（FR-210）") @RequestParam(value = "swapPreference", required = false) SwapPreference swapPreference,
            @Parameter(description = "按卖家过滤（用于个人主页在售列表）") @RequestParam(value = "sellerId", required = false) Long sellerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.list(page,size,sort,categoryId,conditionId,tradeZoneId,keyword,minPrice,maxPrice,swapPreference,sellerId,null,null)));
    }

    /**
     * 发布商品（FR-201）
     *
     * <p>operationId: <code>createItem</code>
     * <p>权限: USER
     * <p>状态转换: '（无） → IN_SALE（I-01）'
     * <p>幂等: required
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "发布商品（FR-201）", operationId = "createItem", description = "发布商品，状态直接置为 `IN_SALE`（假设 A-14：**不使用草稿态**，可传 `DRAFT` 但默认不启用）。  前置条件（不满足返回 403/422）： - 已登录、**未被处罚**（被处罚返回 `403 USER_RESTRICTED_CANNOT_WRITE`）； - **已勾选至少 1 个常用交易区域**，且 `tradeZoneId` ∈ 已勾选区域（否则 `422 TRADE_ZONE_NOT_SELECTED`）； - 至少 1 张图片，最多 9 张（`VALIDATION_INVALID_PARAMETER`）。  **幂等**：必须携带 `Idempotency-Key`（R-17）。同键重复提交只创建一件商品（IDEM-A）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/items")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> createItem(
            @Parameter(description = "**必填**幂等键（R-17）。由前端生成 UUID v4（假设 A-15），同一逻辑操作重试必须复用同一键。 语义：同用户 + 同键 + 同请求体 → 返回首次结果（IDEM-A，HTTP 200/201）； 同键但请求体不同 → `409 COMMON_IDEMPOTENCY_KEY_REUSED`（E-16）。 记录保留 24 小时（假设 A-08）。") @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreateItemRequest body, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(items.create(user.getUserId(),idempotencyKey,body)));
    }

    /**
     * 我的物品（FR-202 前置、PF-08）
     *
     * <p>operationId: <code>listMyItems</code>
     * <p>权限: USER
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "我的物品（FR-202 前置、PF-08）", operationId = "listMyItems", description = "返回当前用户发布的商品，**按状态分组可选**（在售/已预订/交换中/已售出/已下架/违规下架）。 **必须本人**（等价于 `sellerId=当前用户`）。  - 占用态商品在响应中带有 `status` 与（若被订单/交换占用）占用来源提示；   **占用态禁止编辑关键字段**（R-11），前端据此禁用编辑入口。 - `BANNED`（违规下架）商品**不提供重新上架入口**（I-16：仅管理员可恢复）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/items/mine")
    public ResponseEntity<ApiResponse<PageResult<ItemSummary>>> listMyItems(
            @Parameter(description = "页码，**从 0 开始**（R-19：分页与排序在接口层冻结，OOS-09 不做个性化排序）") @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页条数，1–100，默认 20") @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "排序表达式，格式 `字段,方向`（方向 `asc` | `desc`），可重复传入多级排序。 **迭代一允许的排序字段是白名单**（OOS-09 不做推荐/个性化排序）： 商品列表：`published_at`（默认 desc）、`estimated_price`； 订单列表：`created_at`（默认 desc）； 交换提议：`created_at`（默认 desc）； 通知：`created_at`（默认 desc）。 传入白名单外字段返回 `COMMON_INVALID_SORT_FIELD`。") @RequestParam(value = "sort", required = false) List<String> sort,
            @Parameter(description = "按状态过滤，可重复传入") @RequestParam(value = "status", required = false) List<ItemStatus> status,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.list(page,size,sort,null,null,null,null,null,null,null,user.getUserId(),user.getUserId(),status)));
    }

    /**
     * 商品详情（FR-210、PF-02）
     *
     * <p>operationId: <code>getItemDetail</code>
     * <p>权限: GUEST
     * <p><b>[PUBLIC]</b> 无需认证
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "商品详情（FR-210、PF-02）", operationId = "getItemDetail", description = "**访客可用**。返回图片、成色、估价、所在区域、卖家交换状态（FR-210）。  - `estimatedPrice` 附带固定免责声明「用户自填估价，仅供参考，非平台定价」（R-13）。 - `swapDisplayStatus` 为展示用文案（可交换 / 暂不交换 / 交换中 / 已交换）。 - 已删除商品返回 `404 ITEM_NOT_FOUND`。 - **不返回卖家联系方式**（无该字段，OOS-08 不做聊天）。")
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetail>> getItemDetail(
            @Parameter(description = "商品 ID") @PathVariable("itemId") Long itemId, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.detail(itemId,user==null?null:user.getUserId())));
    }

    /**
     * 编辑商品（FR-202）
     *
     * <p>operationId: <code>updateItem</code>
     * <p>权限: USER
     * <p>状态转换: 'IN_SALE→IN_SALE / OFF_SHELF→OFF_SHELF（I-03/I-04）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "编辑商品（FR-202）", operationId = "updateItem", description = "**仅本人**（非本人返回 `403 AUTH_NOT_RESOURCE_OWNER`，**刻意不返回 404**，E-02）。  状态限制（R-11 / 商品状态机 I-03/I-04）： - `IN_SALE` / `OFF_SHELF`：可编辑； - **`RESERVED` / `SWAPPING`（占用态）：禁止修改**标题、估价、分类、成色、交易区域，   请求中包含这些字段返回 `409 ITEM_LOCKED_BY_OCCUPATION`；描述与图片仍可改； - `SOLD` / `BANNED`：终态，任何编辑返回 `409 ITEM_STATUS_NOT_EDITABLE`。  **并发**：请求体 `version` 必填，服务端执行条件更新（R-18）；版本不匹配返回 `409 COMMON_VERSION_CONFLICT`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/items/{itemId}")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> updateItem(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody UpdateItemRequest body, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.update(user.getUserId(),itemId,body)));
    }

    /**
     * 下架商品（FR-203）
     *
     * <p>operationId: <code>takeItemOffShelf</code>
     * <p>权限: USER
     * <p>状态转换: 'IN_SALE → OFF_SHELF（I-05）'
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "下架商品（FR-203）", operationId = "takeItemOffShelf", description = "**仅本人**。`IN_SALE` → `OFF_SHELF`（I-05）。**幂等**：重复下架返回成功态（IDEM-A）。  - 存在**进行中占用**时不可下架（需先终结交易），返回 `409 ITEM_LOCKED_BY_OCCUPATION`。 - **卖家主动下架 != 违规下架**：主动下架后可用 `POST /items/{itemId}/relist` 恢复（I-06）。 - `BANNED`（违规下架）不可通过本接口改变。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/items/{itemId}/off-shelf")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> takeItemOffShelf(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.shelf(user.getUserId(),itemId)));
    }

    /**
     * 重新上架（FR-204）
     *
     * <p>operationId: <code>relistItem</code>
     * <p>权限: USER
     * <p>状态转换: 'OFF_SHELF → IN_SALE（I-06）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "重新上架（FR-204）", operationId = "relistItem", description = "**仅本人**。`OFF_SHELF` → `IN_SALE`（I-06）。**幂等**：重复上架返回成功态。  - 商品若**已被占用**或存在冲突，返回 `409 ITEM_LOCKED_BY_OCCUPATION`（FR-204：已被占用则不可上架）。 - **`BANNED`（违规下架）不可自行上架**：返回 `409 ITEM_BANNED_CANNOT_RELIST`，   仅管理员可通过 `POST /admin/items/{itemId}/restore` 恢复（I-16）。 - **被处罚用户禁止重新上架**（交易写入），返回 `403 USER_RESTRICTED_CANNOT_WRITE`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/items/{itemId}/relist")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> relistItem(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.relist(user.getUserId(),itemId)));
    }

    /**
     * 设置商品交换意愿（FR-401 / R-20）
     *
     * <p>operationId: <code>updateItemSwapPreference</code>
     * <p>权限: USER
     * <p>状态转换: '不改商品 status，仅改 swap_preference（R-20）'
     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "设置商品交换意愿（FR-401 / R-20）", operationId = "updateItemSwapPreference", description = "**仅本人**。设置该商品「接受交换 / 不接受交换」。  - **占用态（`RESERVED`/`SWAPPING`）与终态（`SOLD`/`BANNED`）不可改为 `ACCEPTING`**，   返回 `409 ITEM_STATUS_NOT_SWAPPABLE`。 - **被处罚用户禁止**（交易写入）。 - 幂等：重复设置同一值结果一致（IDEM-A）。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/items/{itemId}/swap-preference")
    // 幂等键 Idempotency-Key 见方法参数（R-17）
    public ResponseEntity<ApiResponse<ItemDetail>> updateItemSwapPreference(
            @Parameter(description = "") @PathVariable("itemId") Long itemId,
            @Parameter(description = "**可选**幂等键（R-17）。建议携带；未携带时服务端仍以状态前置条件 + 条件更新保证不产生重复副作用。") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody UpdateSwapPreferenceRequest body, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.preference(user.getUserId(),itemId,body)));
    }

    /**
     * 查看商品当前占用情况（permission-matrix 3.11）
     *
     * <p>operationId: <code>getItemOccupation</code>
     * <p>权限: USER, ADMIN
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "查看商品当前占用情况（permission-matrix 3.11）", operationId = "getItemOccupation", description = "查看商品是否被占用、被谁占用（订单或交换）。**卖家本人或管理员**可查看； 买家仅能通过自己的订单详情看到自己的占用。  - 占用是**互斥**的：同一商品同时最多一个进行中占用（C-01 / R-23），   因此 `occupation` 要么为 `null` 要么为单个对象。 - 非本人且非管理员返回 `403 AUTH_NOT_RESOURCE_OWNER`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/items/{itemId}/occupation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItemOccupation(
            @Parameter(description = "") @PathVariable("itemId") Long itemId, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(items.occupation(user.getUserId(),itemId,user.isAdmin())));
    }

    /**
     * 上传图片（FR-205 图片能力）
     *
     * <p>operationId: <code>uploadImage</code>
     * <p>权限: USER, ADMIN, RESTRICTED
     *
     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）
     */
    @Operation(summary = "上传图片（FR-205 图片能力）", operationId = "uploadImage", description = "上传商品图片或头像，返回可访问 URL。**需登录**（被处罚用户**允许**上传，用于举报举证等场景后由管理员处理； 实际用于发布商品时会被发布接口拦截）。  限制（假设 A-05）： - 格式：JPG / PNG / WebP； - 单张大小 ≤ **5MB**； - 商品图每商品 1–9 张、头像 1 张（在关联时校验数量）。  存储：**服务端本地目录**，通过统一静态路径访问（OOS-18 不使用对象存储/CDN）。 超限返回 `422 FILE_TOO_LARGE` / `422 FILE_TYPE_NOT_ALLOWED`。")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/files/images")
    public ResponseEntity<ApiResponse<ItemImageUploadResponse>> uploadImage(@RequestParam("file") MultipartFile file,
            @RequestParam(value="purpose", required=false, defaultValue="ITEM_IMAGE") String purpose) {
        if(!"ITEM_IMAGE".equals(purpose)&&!"AVATAR".equals(purpose)) throw new com.jc2001.campustrade.common.DomainException(com.jc2001.campustrade.common.ErrorCode.VALIDATION_ERROR);
        if(file==null||file.isEmpty()) throw new com.jc2001.campustrade.common.DomainException(com.jc2001.campustrade.common.ErrorCode.VALIDATION_ERROR);
        if(file.getSize()>5242880) throw new com.jc2001.campustrade.common.DomainException(com.jc2001.campustrade.common.ErrorCode.FILE_TOO_LARGE);
        String type=file.getContentType(); if(!java.util.Arrays.asList("image/jpeg","image/png","image/webp").contains(type)) throw new com.jc2001.campustrade.common.DomainException(com.jc2001.campustrade.common.ErrorCode.FILE_TYPE_NOT_ALLOWED);
        String ext="image/png".equals(type)?".png":"image/webp".equals(type)?".webp":".jpg"; String name=UUID.randomUUID()+ext;
        try { Path root=Paths.get(uploadDir).toAbsolutePath().normalize();Files.createDirectories(root);Path target=root.resolve(name).normalize();if(!target.startsWith(root))throw new java.io.IOException("invalid path");Files.copy(file.getInputStream(),target,StandardCopyOption.REPLACE_EXISTING);ItemImageUploadResponse out=new ItemImageUploadResponse();out.setImageUrl("/static/uploads/"+name);out.setContentType(type);out.setFileSize((int)file.getSize());return ResponseEntity.status(201).body(ApiResponse.ok(out)); }
        catch(java.io.IOException ex){throw new IllegalStateException("图片保存失败",ex);}
    }

}
