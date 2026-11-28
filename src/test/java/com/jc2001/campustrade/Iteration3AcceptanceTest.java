package com.jc2001.campustrade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 迭代三（切片 3）验收测试：购物车 / 订单 / 交换，共 30 个 operationId。
 *
 * <p>覆盖 2→3 交接文档 §5 的验收清单：
 * <ul>
 *   <li>3A「购物车 → 下单占用 → 模拟付款 → 选点 → 送达 → 确认收货」完整路径</li>
 *   <li>并发下单防超卖（恰好一单成功）</li>
 *   <li>幂等键重放（同键同体返回同一资源）</li>
 *   <li>3B 交换包 / 提议 / 整包接受 / 交换码一次性 / 双方确认 / 互评 / 反提案仅一轮 / 72h 过期</li>
 *   <li>401 / 403 / 404 / 409 / 422 语义</li>
 * </ul>
 *
 * <p>运行前提：本地 PostgreSQL（application.yml 默认 ``campus_trade`` 库），Flyway 自动建表。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.internal.token=test-internal-token"
})
class Iteration3AcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();

    // =================================================================
    // 3A 购买链路
    // =================================================================

    @Test
    @DisplayName("3A 全链路：加购 → 下单占用 → 付款 → 选点 → 交付 → 收货（商品 SOLD）")
    void purchaseFlowEndToEnd() throws Exception {
        String seller = newUser("seller");
        String buyer = newUser("buyer");
        long itemId = newItem(seller, "全链路商品", "NOT_ACCEPTING", 1);

        // 加购（幂等：重复加入返回同一条目）
        JsonNode cart1 = call(HttpMethod.POST, "/api/v1/cart/items", Map.of("itemId", itemId), buyer, null);
        assertEquals(201, statusOf(cart1));
        long cartItemId = cart1.path("data").path("id").asLong();
        JsonNode cart2 = call(HttpMethod.POST, "/api/v1/cart/items", Map.of("itemId", itemId), buyer, null);
        assertEquals(cartItemId, cart2.path("data").path("id").asLong(), "重复加购应幂等，不产生第二条");

        JsonNode cartView = call(HttpMethod.GET, "/api/v1/cart", null, buyer, null);
        assertEquals(200, statusOf(cartView));
        assertEquals(1, cartView.path("data").path("totalItems").asInt());

        // 下单：R-16 下单即占用
        String orderKey = "order-" + UUID.randomUUID();
        JsonNode submitted = call(HttpMethod.POST, "/api/v1/orders",
                Map.of("cartItemIds", List.of(cartItemId), "tradeZoneId", 1), buyer, orderKey);
        assertEquals(201, statusOf(submitted));
        long orderId = submitted.path("data").path("orders").get(0).path("id").asLong();
        assertEquals("PENDING_PAYMENT", submitted.path("data").path("orders").get(0).path("status").asText());
        assertTrue(submitted.path("data").path("orders").get(0).path("remainingSeconds").asLong() > 0,
                "付款倒计时应由服务端计算");
        assertEquals("RESERVED", itemStatus(itemId));

        // 幂等重放：同键同体返回同一订单
        JsonNode replay = call(HttpMethod.POST, "/api/v1/orders",
                Map.of("cartItemIds", List.of(cartItemId), "tradeZoneId", 1), buyer, orderKey);
        assertEquals(orderId, replay.path("data").path("orders").get(0).path("id").asLong(),
                "同键重放不得重复拆单");

        // 付款（幂等）
        String payKey = "pay-" + UUID.randomUUID();
        JsonNode paid = call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/mock-pay", Map.of("confirm", true), buyer, payKey);
        assertEquals(200, statusOf(paid));
        assertEquals("PAID", paid.path("data").path("status").asText());
        assertEquals("模拟付款，未产生真实扣款", paid.path("data").path("mockNotice").asText());
        JsonNode paidAgain = call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/mock-pay", Map.of("confirm", true), buyer, payKey);
        assertEquals(200, statusOf(paidAgain));

        // 未选点不可交付
        JsonNode tooEarly = call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/deliver", null, seller, null);
        assertEquals(409, statusOf(tooEarly));
        assertEquals("ORDER_PICKUP_POINT_NOT_SELECTED", codeOf(tooEarly));

        // 选点（取货点必须属于订单区域）
        long pickupPointId = firstPickupPoint(1);
        JsonNode picked = call(HttpMethod.PUT, "/api/v1/orders/" + orderId + "/pickup-point",
                Map.of("pickupPointId", pickupPointId), buyer, null);
        assertEquals(200, statusOf(picked));

        // 交付 / 收货
        assertEquals(200, statusOf(call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/deliver", null, seller, null)));
        JsonNode completed = call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/confirm-receipt", null, buyer, null);
        assertEquals(200, statusOf(completed));
        assertEquals("COMPLETED", completed.path("data").path("status").asText());
        assertEquals("SOLD", itemStatus(itemId), "确认收货后商品应进入 SOLD 终态");

        // 详情：参与方可读，非参与方 403
        assertEquals(200, statusOf(call(HttpMethod.GET, "/api/v1/orders/" + orderId, null, buyer, null)));
        String outsider = newUser("outsider");
        assertEquals(403, statusOf(call(HttpMethod.GET, "/api/v1/orders/" + orderId, null, outsider, null)));
    }

    @Test
    @DisplayName("3A 取消：未付款可取消并释放占用；已付款不可取消")
    void cancelOrderReleasesOccupation() throws Exception {
        String seller = newUser("cseller");
        String buyer = newUser("cbuyer");
        long itemId = newItem(seller, "取消商品", "NOT_ACCEPTING", 1);
        long cartItemId = addCart(buyer, itemId);
        long orderId = submitOrder(buyer, cartItemId, 1);

        assertEquals("RESERVED", itemStatus(itemId));
        assertEquals(200, statusOf(call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/cancel", Map.of("reason", "不想要"), buyer, null)));
        assertEquals("IN_SALE", itemStatus(itemId), "取消后商品应释放回 IN_SALE");
        // 重复取消幂等
        assertEquals(200, statusOf(call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/cancel", Map.of("reason", "再次"), buyer, null)));

        // 已付款不可取消
        long cart2 = addCart(buyer, itemId);
        long order2 = submitOrder(buyer, cart2, 1);
        call(HttpMethod.POST, "/api/v1/orders/" + order2 + "/mock-pay", Map.of("confirm", true), buyer, "pay-" + UUID.randomUUID());
        JsonNode cannot = call(HttpMethod.POST, "/api/v1/orders/" + order2 + "/cancel", Map.of("reason", "x"), buyer, null);
        assertEquals(409, statusOf(cannot));
        assertEquals("ORDER_CANNOT_CANCEL_AFTER_PAID", codeOf(cannot));
    }

    @Test
    @DisplayName("3A 并发：5 个买家抢同一商品，恰好 1 单成功、占用唯一")
    void concurrentSubmitOnlyOneWins() throws Exception {
        String seller = newUser("rseller");
        long itemId = newItem(seller, "抢购商品", "NOT_ACCEPTING", 1);

        List<String> buyers = new ArrayList<>();
        List<Long> cartIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String token = newUser("racer" + i);
            buyers.add(token);
            cartIds.add(addCart(token, itemId));
        }

        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final String token = buyers.get(i);
            final long cartId = cartIds.get(i);
            final String key = "race-" + UUID.randomUUID();
            tasks.add(() -> statusOf(call(HttpMethod.POST, "/api/v1/orders",
                    Map.of("cartItemIds", List.of(cartId), "tradeZoneId", 1), token, key)));
        }
        List<Future<Integer>> futures = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
        pool.shutdownNow();

        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        for (Future<Integer> f : futures) {
            int status = f.get();
            if (status == 201) created.incrementAndGet();
            if (status == 409) conflict.incrementAndGet();
        }
        assertEquals(1, created.get(), "并发下单只允许 1 单成功");
        assertEquals(4, conflict.get(), "其余应为 409 占用冲突");
        assertEquals("RESERVED", itemStatus(itemId));

        Integer active = jdbc.queryForObject(
                "SELECT count(*) FROM item_occupations WHERE item_id=? AND released_at IS NULL", Integer.class, itemId);
        assertEquals(1, active, "同一商品同时最多一条进行中占用");
    }

    @Test
    @DisplayName("3A 超时释放：过期订单不可付款，任务置 TIMEOUT_CANCELLED 并释放商品")
    void timeoutReleaseTask() throws Exception {
        String seller = newUser("tseller");
        String buyer = newUser("tbuyer");
        long itemId = newItem(seller, "超时商品", "NOT_ACCEPTING", 1);
        long cartItemId = addCart(buyer, itemId);
        long orderId = submitOrder(buyer, cartItemId, 1);

        jdbc.update("UPDATE orders SET expires_at = now() - interval '1 minute' WHERE id=?", orderId);

        JsonNode pay = call(HttpMethod.POST, "/api/v1/orders/" + orderId + "/mock-pay", Map.of("confirm", true),
                buyer, "pay-" + UUID.randomUUID());
        assertEquals(409, statusOf(pay));
        assertEquals("ORDER_TIMEOUT_RELEASED", codeOf(pay));

        JsonNode task = callWithHeaders(HttpMethod.POST, "/api/v1/internal/tasks/order-timeout-release", null,
                Map.of("X-Internal-Token", "test-internal-token"));
        assertEquals(200, statusOf(task));

        JsonNode detail = call(HttpMethod.GET, "/api/v1/orders/" + orderId, null, buyer, null);
        assertEquals("TIMEOUT_CANCELLED", detail.path("data").path("status").asText());
        assertEquals("PAYMENT_TIMEOUT", detail.path("data").path("cancelReason").asText());
        assertEquals("IN_SALE", itemStatus(itemId));
    }

    // =================================================================
    // 3B 交换链路
    // =================================================================

    @Test
    @DisplayName("3B 全链路：建包 → 提议 → 接受 → 交换码 → 核销 → 双方确认 → 互评")
    void swapFlowEndToEnd() throws Exception {
        String a = newUser("swapa");
        String b = newUser("swapb");
        long a1 = newItem(a, "A1", "ACCEPTING", 1);
        long a2 = newItem(a, "A2", "ACCEPTING", 1);
        long b1 = newItem(b, "B1", "ACCEPTING", 1);

        long bundleA = createBundle(a, List.of(a1, a2));

        JsonNode proposal = call(HttpMethod.POST, "/api/v1/swap-proposals",
                Map.of("proposerBundleId", bundleA, "targetItemIds", List.of(b1), "tradeZoneId", 1),
                a, "proposal-" + UUID.randomUUID());
        assertEquals(201, statusOf(proposal));
        long proposalId = proposal.path("data").path("id").asLong();
        assertEquals("PENDING", proposal.path("data").path("status").asText());
        assertEquals("SWAPPING", itemStatus(a1), "提议期间双方商品均被占用");

        // 目标方整包接受 → 生成交换码
        JsonNode accepted = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/accept", null,
                b, "accept-" + UUID.randomUUID());
        assertEquals(200, statusOf(accepted));
        assertEquals("ACCEPTED", accepted.path("data").path("status").asText());
        String code = accepted.path("data").path("swapCode").path("code").asText();
        assertNotNull(code);

        // 重复接受应失败（E-09）
        JsonNode acceptAgain = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/accept", null,
                b, "accept2-" + UUID.randomUUID());
        assertEquals(409, statusOf(acceptAgain));
        assertEquals("SWAP_PROPOSAL_ALREADY_ACCEPTED", codeOf(acceptAgain));

        // 未核销不可确认
        JsonNode early = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/confirm", Map.of("confirm", true), a, null);
        assertEquals(409, statusOf(early));
        assertEquals("SWAP_CODE_NOT_VERIFIED", codeOf(early));

        // 错误码 / 正确核销 / 重复核销
        assertEquals(422, statusOf(call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/swap-code/verify",
                Map.of("code", "000000"), a, null)));
        JsonNode verified = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/swap-code/verify",
                Map.of("code", code), a, null);
        assertEquals(200, statusOf(verified));
        assertEquals("VERIFIED", verified.path("data").path("status").asText());
        JsonNode verifyAgain = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/swap-code/verify",
                Map.of("code", code), a, null);
        assertEquals(409, statusOf(verifyAgain));
        assertEquals("SWAP_CODE_ALREADY_VERIFIED", codeOf(verifyAgain));

        // 双方确认：一方 ACCEPTED，齐备后 COMPLETED + 商品 SOLD
        JsonNode one = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/confirm", Map.of("confirm", true), a, null);
        assertEquals("ACCEPTED", one.path("data").path("status").asText());
        assertTrue(one.path("data").path("myConfirmed").asBoolean());
        JsonNode two = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/confirm", Map.of("confirm", true), b, null);
        assertEquals("COMPLETED", two.path("data").path("status").asText());
        assertEquals("SOLD", itemStatus(a1));
        assertEquals("SOLD", itemStatus(b1));

        // 互评：一次成功、重复 409
        JsonNode review = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/reviews",
                Map.of("rating", 5, "content", "顺利"), a, "review-" + UUID.randomUUID());
        assertEquals(201, statusOf(review));
        JsonNode reviewAgain = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/reviews",
                Map.of("rating", 4, "content", "再来一次"), a, "review2-" + UUID.randomUUID());
        assertEquals(409, statusOf(reviewAgain));
        assertEquals("REVIEW_ALREADY_SUBMITTED", codeOf(reviewAgain));
    }

    @Test
    @DisplayName("3B 拒绝：释放双方占用，交换包回到可编辑状态")
    void swapRejectReleasesOccupation() throws Exception {
        String a = newUser("reja");
        String b = newUser("rejb");
        long a1 = newItem(a, "RA1", "ACCEPTING", 1);
        long b1 = newItem(b, "RB1", "ACCEPTING", 1);
        long bundle = createBundle(a, List.of(a1));

        long proposalId = createProposal(a, bundle, List.of(b1));
        JsonNode rejected = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/reject",
                Map.of("reason", "不换"), b, null);
        assertEquals(200, statusOf(rejected));
        assertEquals("REJECTED", rejected.path("data").path("status").asText());
        assertEquals("TARGET_REJECTED", rejected.path("data").path("terminationReason").asText());
        assertEquals("IN_SALE", itemStatus(a1));
        assertEquals("IN_SALE", itemStatus(b1));
    }

    @Test
    @DisplayName("3B 反提案：仅允许一轮，原提议置 COUNTERED 并释放占用")
    void swapCounterOnlyOnce() throws Exception {
        String a = newUser("cna");
        String b = newUser("cnb");
        long a1 = newItem(a, "CA1", "ACCEPTING", 1);
        long b1 = newItem(b, "CB1", "ACCEPTING", 1);
        long b2 = newItem(b, "CB2", "ACCEPTING", 1);
        long bundleA = createBundle(a, List.of(a1));
        long bundleB = createBundle(b, List.of(b2));

        long proposalId = createProposal(a, bundleA, List.of(b1));
        JsonNode countered = call(HttpMethod.POST, "/api/v1/swap-proposals/" + proposalId + "/counter",
                Map.of("targetBundleId", bundleB, "message", "换我这个"), b, "counter-" + UUID.randomUUID());
        assertEquals(201, statusOf(countered));
        assertEquals(1, countered.path("data").path("counterDepth").asInt());
        long newProposalId = countered.path("data").path("id").asLong();

        JsonNode original = call(HttpMethod.GET, "/api/v1/swap-proposals/" + proposalId, null, a, null);
        assertEquals("COUNTERED", original.path("data").path("status").asText());

        // 反提案的反提案：409
        JsonNode second = call(HttpMethod.POST, "/api/v1/swap-proposals/" + newProposalId + "/counter",
                Map.of("targetBundleId", bundleA), a, "counter2-" + UUID.randomUUID());
        assertEquals(409, statusOf(second));
        assertEquals("SWAP_COUNTER_NOT_ALLOWED", codeOf(second));
    }

    @Test
    @DisplayName("3B 过期：72 小时任务将提议置 EXPIRED 并释放全部占用")
    void swapExpireTask() throws Exception {
        String a = newUser("expa");
        String b = newUser("expb");
        long a1 = newItem(a, "EA1", "ACCEPTING", 1);
        long b1 = newItem(b, "EB1", "ACCEPTING", 1);
        long bundle = createBundle(a, List.of(a1));
        long proposalId = createProposal(a, bundle, List.of(b1));

        jdbc.update("UPDATE swap_proposals SET expires_at = now() - interval '1 minute' WHERE id=?", proposalId);

        JsonNode task = callWithHeaders(HttpMethod.POST, "/api/v1/internal/tasks/swap-proposal-expire", null,
                Map.of("X-Internal-Token", "test-internal-token"));
        assertEquals(200, statusOf(task));
        assertTrue(task.path("data").path("processed").asInt() >= 1);

        JsonNode detail = call(HttpMethod.GET, "/api/v1/swap-proposals/" + proposalId, null, a, null);
        assertEquals("EXPIRED", detail.path("data").path("status").asText());
        assertEquals("EXPIRED_72H", detail.path("data").path("terminationReason").asText());
        assertEquals("IN_SALE", itemStatus(a1));
        assertEquals("IN_SALE", itemStatus(b1));
    }

    @Test
    @DisplayName("3B 交换包：件数越界 422、含他人商品 422、编辑版本冲突 409、删除幂等")
    void swapBundleRules() throws Exception {
        String a = newUser("bda");
        String b = newUser("bdb");
        long a1 = newItem(a, "BA1", "ACCEPTING", 1);
        long a2 = newItem(a, "BA2", "ACCEPTING", 1);
        long a3 = newItem(a, "BA3", "ACCEPTING", 1);
        long a4 = newItem(a, "BA4", "ACCEPTING", 1);
        long a5 = newItem(a, "BA5", "ACCEPTING", 1);
        long a6 = newItem(a, "BA6", "ACCEPTING", 1);
        long other = newItem(b, "BB1", "ACCEPTING", 1);

        JsonNode tooMany = call(HttpMethod.POST, "/api/v1/swap-bundles",
                Map.of("itemIds", List.of(a1, a2, a3, a4, a5, a6), "tradeZoneId", 1), a, "bd-" + UUID.randomUUID());
        assertEquals(422, statusOf(tooMany));
        assertEquals("SWAP_BUNDLE_ITEM_COUNT_OUT_OF_RANGE", codeOf(tooMany));

        JsonNode notOwned = call(HttpMethod.POST, "/api/v1/swap-bundles",
                Map.of("itemIds", List.of(a1, other), "tradeZoneId", 1), a, "bd-" + UUID.randomUUID());
        assertEquals(422, statusOf(notOwned));
        assertEquals("SWAP_BUNDLE_ITEM_NOT_OWNED", codeOf(notOwned));

        JsonNode created = call(HttpMethod.POST, "/api/v1/swap-bundles",
                Map.of("itemIds", List.of(a1, a2), "title", "包", "tradeZoneId", 1), a, "bd-" + UUID.randomUUID());
        assertEquals(201, statusOf(created));
        long bundleId = created.path("data").path("id").asLong();
        int version = created.path("data").path("version").asInt();

        JsonNode edited = call(HttpMethod.PUT, "/api/v1/swap-bundles/" + bundleId,
                Map.of("itemIds", List.of(a1, a3), "version", version), a, null);
        assertEquals(200, statusOf(edited));

        JsonNode conflict = call(HttpMethod.PUT, "/api/v1/swap-bundles/" + bundleId,
                Map.of("itemIds", List.of(a1), "version", version), a, null);
        assertEquals(409, statusOf(conflict));

        assertEquals(204, statusOf(call(HttpMethod.DELETE, "/api/v1/swap-bundles/" + bundleId, null, a, null)));
        assertEquals(204, statusOf(call(HttpMethod.DELETE, "/api/v1/swap-bundles/" + bundleId, null, a, null)),
                "重复删除应幂等返回 204");
    }

    @Test
    @DisplayName("鉴权与参数：未登录 401、缺幂等头 400、非法排序 400、越权 403")
    void authAndValidation() throws Exception {
        String user = newUser("val");
        long itemId = newItem(user, "校验商品", "NOT_ACCEPTING", 1);

        assertEquals(401, statusOf(call(HttpMethod.GET, "/api/v1/cart", null, null, null)));

        JsonNode missingHeader = call(HttpMethod.POST, "/api/v1/swap-bundles",
                Map.of("itemIds", List.of(itemId), "tradeZoneId", 1), user, null);
        assertEquals(400, statusOf(missingHeader), "缺必填请求头应为 400 而非 500");

        assertEquals(400, statusOf(call(HttpMethod.GET, "/api/v1/orders?sort=price,desc", null, user, null)));

        // 加自己的商品 → 422
        JsonNode own = call(HttpMethod.POST, "/api/v1/cart/items", Map.of("itemId", itemId), user, null);
        assertEquals(422, statusOf(own));
        assertEquals("CART_OWN_ITEM_NOT_ALLOWED", codeOf(own));

        // 自己换自己 → 422
        long bundle = createBundle(user, List.of(itemId));
        JsonNode self = call(HttpMethod.POST, "/api/v1/swap-proposals",
                Map.of("proposerBundleId", bundle, "targetItemIds", List.of(itemId), "tradeZoneId", 1),
                user, "self-" + UUID.randomUUID());
        assertEquals(422, statusOf(self));
        assertEquals("SWAP_SELF_PROPOSAL_NOT_ALLOWED", codeOf(self));
    }

    // =================================================================
    // 辅助方法
    // =================================================================

    private String newUser(String prefix) throws Exception {
        String username = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        JsonNode registered = call(HttpMethod.POST, "/api/v1/auth/register",
                Map.of("username", username, "email", username + "@example.com",
                        "password", "Passw0rd123", "nickname", prefix), null, null);
        assertEquals(201, statusOf(registered), "注册失败: " + registered);
        JsonNode loggedIn = call(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("username", username, "password", "Passw0rd123"), null, null);
        assertEquals(200, statusOf(loggedIn));
        String token = loggedIn.path("data").path("accessToken").asText();
        JsonNode zones = call(HttpMethod.PUT, "/api/v1/users/me/trade-zones",
                Map.of("tradeZoneIds", List.of(1, 2)), token, null);
        assertEquals(200, statusOf(zones));
        return token;
    }

    private long newItem(String token, String title, String swapPreference, long zone) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "acceptance");
        body.put("categoryId", 1);
        body.put("conditionId", 1);
        body.put("tradeZoneId", zone);
        body.put("estimatedPrice", "30.00");
        body.put("imageUrls", List.of("/static/uploads/x.png"));
        body.put("swapPreference", swapPreference);
        JsonNode created = call(HttpMethod.POST, "/api/v1/items", body, token, "item-" + UUID.randomUUID());
        assertEquals(201, statusOf(created), "创建商品失败: " + created);
        return created.path("data").path("id").asLong();
    }

    private long addCart(String token, long itemId) throws Exception {
        JsonNode cart = call(HttpMethod.POST, "/api/v1/cart/items", Map.of("itemId", itemId), token, null);
        assertEquals(201, statusOf(cart), "加购失败: " + cart);
        return cart.path("data").path("id").asLong();
    }

    private long submitOrder(String token, long cartItemId, long zone) throws Exception {
        JsonNode submitted = call(HttpMethod.POST, "/api/v1/orders",
                Map.of("cartItemIds", List.of(cartItemId), "tradeZoneId", zone), token, "order-" + UUID.randomUUID());
        assertEquals(201, statusOf(submitted), "下单失败: " + submitted);
        return submitted.path("data").path("orders").get(0).path("id").asLong();
    }

    private long createBundle(String token, List<Long> itemIds) throws Exception {
        JsonNode bundle = call(HttpMethod.POST, "/api/v1/swap-bundles",
                Map.of("itemIds", itemIds, "title", "包", "tradeZoneId", 1), token, "bundle-" + UUID.randomUUID());
        assertEquals(201, statusOf(bundle), "建包失败: " + bundle);
        return bundle.path("data").path("id").asLong();
    }

    private long createProposal(String token, long bundleId, List<Long> targetItemIds) throws Exception {
        JsonNode proposal = call(HttpMethod.POST, "/api/v1/swap-proposals",
                Map.of("proposerBundleId", bundleId, "targetItemIds", targetItemIds, "tradeZoneId", 1),
                token, "proposal-" + UUID.randomUUID());
        assertEquals(201, statusOf(proposal), "提议失败: " + proposal);
        return proposal.path("data").path("id").asLong();
    }

    private long firstPickupPoint(long zoneId) throws Exception {
        JsonNode points = call(HttpMethod.GET, "/api/v1/trade-zones/" + zoneId + "/pickup-points", null, null, null);
        assertEquals(200, statusOf(points));
        assertTrue(points.path("data").size() > 0, "交易区域应有取货点种子数据");
        return points.path("data").get(0).path("id").asLong();
    }

    private String itemStatus(long itemId) throws Exception {
        JsonNode detail = call(HttpMethod.GET, "/api/v1/items/" + itemId, null, null, null);
        assertEquals(200, statusOf(detail));
        return detail.path("data").path("status").asText();
    }

    /** 统一调用：返回把 HTTP 状态塞进 ``status`` 字段的 JSON，避免异常分支写法。 */
    private JsonNode call(HttpMethod method, String path, Object body, String token, String idempotencyKey) throws Exception {
        Map<String, String> headers = new HashMap<>();
        if (idempotencyKey != null) headers.put("Idempotency-Key", idempotencyKey);
        return callWithHeaders(method, path, body, headers, token);
    }

    private JsonNode callWithHeaders(HttpMethod method, String path, Object body, Map<String, String> extraHeaders) throws Exception {
        return callWithHeaders(method, path, body, extraHeaders, null);
    }

    private JsonNode callWithHeaders(HttpMethod method, String path, Object body,
                                     Map<String, String> extraHeaders, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        if (extraHeaders != null) extraHeaders.forEach(headers::set);
        HttpEntity<String> entity = new HttpEntity<>(
                body == null ? null : mapper.writeValueAsString(body), headers);

        ResponseEntity<String> response = rest.exchange("http://localhost:" + port + path, method, entity, String.class);
        HttpStatus status = response.getStatusCode();
        String payload = response.getBody();
        com.fasterxml.jackson.databind.node.ObjectNode node = payload == null || payload.isBlank()
                ? mapper.createObjectNode()
                : (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(payload);
        node.put("status", status.value());
        return node;
    }

    private int statusOf(JsonNode node) {
        return node.path("status").asInt();
    }

    private String codeOf(JsonNode node) {
        return node.path("code").asText();
    }
}
