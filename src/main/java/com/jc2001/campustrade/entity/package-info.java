/**
 * JPA 实体层（占位包）。
 *
 * <p><b>本步骤为空</b>：骨架阶段不创建实体。</p>
 *
 * <p><b>重要说明</b>：{@code spring.jpa.hibernate.ddl-auto=validate}。
 * 当前无实体，因此 validate 无校验目标，不会失败。
 * 下一步新增实体时必须保证**与 {@code V1__init_schema.sql} 完全一致**，
 * 否则应用启动会因 schema 校验失败而中断。</p>
 *
 * <p><b>不准做的事</b>：不得修改 {@code V1}~{@code V4} 迁移脚本
 * （Flyway checksum 会不一致）；表结构变更必须新增 {@code V5+}。</p>
 *
 * <p><b>生成依据</b>：{@code docs/iteration1/design/schema.md}（字段字典）、
 * {@code design/er-diagram.md}（关系图）。</p>
 *
 * <p><b>字段约定</b>：
 * <ul>
 *   <li>乐观锁列 {@code version} 用 {@code @Version}（12 张表有该列，R-18）</li>
 *   <li>金额列 {@code numeric(12,2)} 用 {@code BigDecimal}，序列化为字符串（R-13）</li>
 *   <li>时间列统一 {@code timestamptz} → {@code OffsetDateTime} / {@code Instant}（R-19）</li>
 *   <li>PostgreSQL ENUM 列使用 {@code @Enumerated(EnumType.STRING)}，
 *       枚举取值必须与 {@code V1} 逐字一致（AD-09）</li>
 * </ul>
 */
package com.jc2001.campustrade.entity;
