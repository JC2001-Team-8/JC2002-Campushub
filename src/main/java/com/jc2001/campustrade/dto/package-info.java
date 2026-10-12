/**
 * 数据传输对象层（占位包）。
 *
 * <p><b>本步骤为空</b>：骨架阶段不创建业务 DTO。</p>
 *
 * <p><b>生成依据</b>：{@code docs/iteration1/design/openapi.yaml} 的
 * {@code components.schemas}（99 个 Schema）。</p>
 *
 * <p><b>契约约束（api-contract.md AD-07/AD-08）</b>：
 * <ul>
 *   <li><b>请求 DTO 与响应 DTO 分离</b>，请求体不得复用数据库实体</li>
 *   <li><b>不暴露内部字段</b>：{@code password_hash} / {@code deleted_at} /
 *       {@code created_by} / {@code updated_by} / {@code request_hash}</li>
 *   <li>枚举取值必须与 {@code V1__init_schema.sql} 一致，不得新增未登记枚举</li>
 *   <li>金额字段使用十进制字符串（{@code DecimalString}），不用 double</li>
 * </ul>
 *
 * <p><b>建议子包</b>：{@code dto.request} / {@code dto.response}，
 * 并在每个 DTO 上保留指向契约 Schema 名的 Javadoc 引用，便于双向追溯。</p>
 */
package com.jc2001.campustrade.dto;
