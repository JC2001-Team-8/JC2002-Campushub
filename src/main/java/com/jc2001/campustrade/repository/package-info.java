/**
 * 数据访问层（占位包）。
 *
 * <p><b>本步骤为空</b>：骨架阶段不创建任何 {@code @Repository} 或实体。
 *
 * <p><b>下一步生成依据</b>：{@code docs/iteration1/design/schema.md}
 * （25 张业务表 / 324 列字段字典）与
 * {@code design/er-diagram.md}（实体关系与基数）。
 *
 * <p><b>关键约束（必须在 Repository 层体现）</b>：
 * <ul>
 *   <li><b>处罚记录、申诉记录、订单、交换记录永不物理删除</b>（R-32）：
 *       这些表无 {@code deleted_at}，只允许状态变更</li>
 *   <li>仅在 {@code items} / {@code swap_bundles} 上使用软删除（{@code deleted_at}）</li>
 *   <li>状态与 {@code version} 的更新必须使用条件更新（{@code WHERE status = ? AND version = ?}），
 *       见 {@code docs/iteration1/design/idempotency-and-concurrency.md} §6</li>
 *   <li>部分唯一索引冲突（SQLState <b>23505</b>）必须映射为对应域的 <b>409</b> 错误码，
 *       而不是 500 —— 映射表见同文档 §7</li>
 * </ul>
 */
package com.jc2001.campustrade.repository;
