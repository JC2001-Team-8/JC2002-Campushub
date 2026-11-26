package com.jc2001.campustrade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 修改个人资料（FR-105 / FR-106）。**被处罚用户在处罚期内禁止修改昵称与头像**（假设 A-13，DQ/Q-12），
但**允许修改常用交易区域**（属于非交易写入，见 permission-matrix §8）。

 *
 * <p>对应契约 {@code components.schemas.UpdateProfileRequest}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。
 */
@Schema(name = "UpdateProfileRequest", description = "修改个人资料（FR-105 / FR-106）。**被处罚用户在处罚期内禁止修改昵称与头像**（假设 A-13，DQ/Q-12）， 但**允许修改常用交易区域**（属于非交易写入，见 permission-matrix §8）。")
public class UpdateProfileRequest {

    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean nicknamePresent;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean avatarUrlPresent;

    /**
     * 昵称，1–20 字符
     */
    @Size(min = 1, max = 20)
    @Schema(description = "昵称，1–20 字符", example = "爱丽丝")
    private String nickname;

    /**
     * 头像 URL（先通过 `POST /files/images` 上传取得）。传 `null` 表示删除头像
     */
    @Size(max = 255)
    @Schema(description = "头像 URL（先通过 `POST /files/images` 上传取得）。传 `null` 表示删除头像", example = "/static/uploads/2026/10/abc.png")
    private String avatarUrl;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public UpdateProfileRequest() {
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.nicknamePresent = true;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.avatarUrlPresent = true;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isNicknamePresent() { return nicknamePresent; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAvatarUrlPresent() { return avatarUrlPresent; }

    @Override
    public String toString() {
        return "UpdateProfileRequest{"
                + "nickname=" + nickname + ", "
                + "avatarUrl=" + avatarUrl + ", "
                + '}';
    }
}
