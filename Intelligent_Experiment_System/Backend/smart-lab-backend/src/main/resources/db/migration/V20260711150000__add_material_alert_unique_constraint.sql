-- ============================================================
-- 物资管理子系统 — 预警记录唯一约束（防并发重复）
-- 版本: V1.2 | 日期: 2026-07-11
-- 修复: 并发场景下两条事务同时通过 existsTodayAlert → insert 产生重复预警
-- 方案: 虚拟列 alert_day + UNIQUE KEY 在 DB 层强制去重
-- ============================================================

-- 1. 添加按天虚拟列（从 alert_time 提取日期部分）
ALTER TABLE material_alert_record
    ADD COLUMN alert_day DATE GENERATED ALWAYS AS (DATE(alert_time)) STORED;

-- 2. 添加唯一约束：同一物资+批次+类型+天 只允许一条预警
ALTER TABLE material_alert_record
    ADD UNIQUE KEY uk_alert_material_batch_type_day (material_id, batch_id, alert_type, alert_day);

-- 3. 移除旧的普通索引（已被唯一约束覆盖）
ALTER TABLE material_alert_record
    DROP INDEX idx_alert_material_batch_type_time;
