package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 通知设置。【待确认 Q-05】当前假设**仅站内通知**。
这是任务书列出的能力，需求基线未含独立 FR —— 已登记为 AQ-01（见 api-open-questions.md）。

 *
 * <p>对应契约 {@code components.schemas.NotificationSettings}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "NotificationSettings", description = "通知设置。【待确认 Q-05】当前假设**仅站内通知**。 这是任务书列出的能力，需求基线未含独立 FR —— 已登记为 AQ-01（见 api-open-questions.md）。")
public class NotificationSettings {

    /**
     * 是否接收站内通知（关闭后仍保留处罚类强制性通知）
     */
    @NotNull
    @Schema(description = "是否接收站内通知（关闭后仍保留处罚类强制性通知）")
    private Boolean inAppEnabled;

    /**
     * 是否接收邮件通知。迭代一**未实现投递**（Q-05），字段为预留，恒为 false
     */
    @Schema(description = "是否接收邮件通知。迭代一**未实现投递**（Q-05），字段为预留，恒为 false")
    private Boolean emailEnabled;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public NotificationSettings() {
    }

    public Boolean getInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(Boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    @Override
    public String toString() {
        return "NotificationSettings{"
                + "inAppEnabled=" + inAppEnabled + ", "
                + "emailEnabled=" + emailEnabled + ", "
                + '}';
    }
}
