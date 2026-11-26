package com.jc2001.campustrade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 金额，**十进制字符串**，单位人民币元，固定 2 位小数（如 `"120.00"`）。
使用字符串而非 number 以避免浮点误差（R-13）。
`items.estimated_price` / `orders.estimated_total` / `*_snapshot` 均为 `numeric(12,2)`。

 *
 * <p>对应契约 {@code components.schemas.DecimalString}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "DecimalString", description = "金额，**十进制字符串**，单位人民币元，固定 2 位小数（如 `\"120.00\"`）。 使用字符串而非 number 以避免浮点误差（R-13）。 `items.estimated_price` / `orders.estimated_total` / `*_snapshot` 均为 `numeric(12,2)`。")
public class DecimalString {

    private String value;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public DecimalString() {
    }

    @JsonCreator
    public DecimalString(String value) {
        this.value = normalize(value);
    }

    public static DecimalString of(BigDecimal value) {
        return value == null ? null : new DecimalString(value.toPlainString());
    }

    private static String normalize(String value) {
        if (value == null) return null;
        return new BigDecimal(value).setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    @JsonValue
    public String getValue() { return value; }

    public void setValue(String value) { this.value = normalize(value); }

    @Override
    public String toString() {
        return value;
    }
}
