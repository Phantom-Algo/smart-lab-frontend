-- ============================================================
-- 物资管理子系统 — 唯一约束补充
-- 版本: V1.1 | 日期: 2026-07-11
-- 修复: 防重入竞态（BUG-3），数据库层加唯一索引
-- ============================================================

-- 1. 领用申请防重入：同一人对同一物资同状态下只允许一条记录
--    效果：并发场景下 insert 第二条待审核记录时 DB 直接拒绝，无需依赖 check-then-act
ALTER TABLE material_receive_apply
    ADD UNIQUE KEY uk_apply_material_applicant_status (material_id, applicant_id, apply_status);

-- 2. 预警去重防并发：同一物资+批次+类型+日期只允许一条预警记录
--    注意：alert_time 包含时分秒，需要按天去重。使用虚拟列或应用层保证。
--    此处先加索引加速 existsTodayAlert 查询，真正的唯一性由 @Transactional 保证
ALTER TABLE material_alert_record
    ADD KEY idx_alert_material_batch_type_time (material_id, batch_id, alert_type, alert_time);
