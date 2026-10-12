package com.jc2001.campustrade.dto.response;

import com.jc2001.campustrade.dto.response.AdminUserSummary;
import com.jc2001.campustrade.dto.response.Appeal;
import com.jc2001.campustrade.dto.response.ItemSummary;
import com.jc2001.campustrade.dto.response.Penalty;
import com.jc2001.campustrade.dto.response.Report;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

// GENERATED FROM openapi.yaml —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程
/**
 * 管理员视角的用户详情与交易概览（permission-matrix 6.2）
 *
 * <p>对应契约 {@code components.schemas.AdminUserDetail}。
 * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。
 */
@Schema(name = "AdminUserDetail", description = "管理员视角的用户详情与交易概览（permission-matrix 6.2）")
public class AdminUserDetail {

    @NotNull
    @Schema(description = "user")
    private AdminUserSummary user;

    @Schema(description = "itemsInSale")
    private List<ItemSummary> itemsInSale;

    @NotNull
    @Schema(description = "penalties")
    private List<Penalty> penalties;

    /**
     * 该用户提交或被举报的记录
     */
    @NotNull
    @Schema(description = "该用户提交或被举报的记录")
    private List<Report> reports;

    @NotNull
    @Schema(description = "appeals")
    private List<Appeal> appeals;

    // -------------------------------------------------------------
    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）
    // -------------------------------------------------------------

    public AdminUserDetail() {
    }

    public AdminUserSummary getUser() {
        return user;
    }

    public void setUser(AdminUserSummary user) {
        this.user = user;
    }

    public List<ItemSummary> getItemsInSale() {
        return itemsInSale;
    }

    public void setItemsInSale(List<ItemSummary> itemsInSale) {
        this.itemsInSale = itemsInSale;
    }

    public List<Penalty> getPenalties() {
        return penalties;
    }

    public void setPenalties(List<Penalty> penalties) {
        this.penalties = penalties;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }

    public List<Appeal> getAppeals() {
        return appeals;
    }

    public void setAppeals(List<Appeal> appeals) {
        this.appeals = appeals;
    }

    @Override
    public String toString() {
        return "AdminUserDetail{"
                + "user=" + user + ", "
                + "itemsInSale=" + itemsInSale + ", "
                + "penalties=" + penalties + ", "
                + "reports=" + reports + ", "
                + "appeals=" + appeals + ", "
                + '}';
    }
}
