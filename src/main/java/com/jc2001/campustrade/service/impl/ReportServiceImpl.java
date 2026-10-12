package com.jc2001.campustrade.service.impl;

import com.jc2001.campustrade.service.ReportService;
import org.springframework.stereotype.Service;
import com.jc2001.campustrade.common.PageResult;
import com.jc2001.campustrade.dto.request.CreateReportRequest;
import com.jc2001.campustrade.dto.response.Report;

// GENERATED FROM openapi.yaml —— 请勿手工修改
/**
 * ReportService 空实现。
 *
 * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，
 * 避免骨架被误认为可用。
 *
 * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。
 */
@Service
public class ReportServiceImpl implements ReportService {

    /**
     * 举报商品（FR-211）（未实现）
     *
     * <p>operationId: <code>reportItem</code>
     */
    @Override
    public Report reportItem(Long itemId, String idempotencyKey, CreateReportRequest body) {
        throw new UnsupportedOperationException(
                "TODO: implement reportItem");
    }

    /**
     * 我提交的举报（permission-matrix 3.13）（未实现）
     *
     * <p>operationId: <code>listMyReports</code>
     */
    @Override
    public PageResult<Report> listMyReports(Integer page, Integer size) {
        throw new UnsupportedOperationException(
                "TODO: implement listMyReports");
    }

    /**
     * 查看我的举报详情（permission-matrix 3.13）（未实现）
     *
     * <p>operationId: <code>getMyReport</code>
     */
    @Override
    public Report getMyReport(Long reportId) {
        throw new UnsupportedOperationException(
                "TODO: implement getMyReport");
    }

}
