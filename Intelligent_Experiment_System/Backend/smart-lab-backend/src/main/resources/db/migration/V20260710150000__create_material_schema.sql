-- ============================================================
-- 物资管理子系统 — 数据库Schema初始化
-- 版本: V1.0
-- 表数: 7张
-- 依赖: personnel_management.person（person_id逻辑外键）
--       lab_management.lab（lab_id逻辑外键）
-- ============================================================

-- 1. 物资表
CREATE TABLE material (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    material_no VARCHAR(64) NOT NULL COMMENT '物资编号，全局唯一',
    material_name VARCHAR(128) NOT NULL COMMENT '物资名称',
    lab_id BIGINT UNSIGNED NOT NULL COMMENT '所属实验室ID，逻辑外键→lab_management.lab.id',
    unit VARCHAR(32) NOT NULL COMMENT '计量单位，如瓶/盒/mL',
    safety_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '安全库存量，触发低库存预警的阈值',
    current_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前在库总量（所有在库批次remaining之和）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_no (material_no),
    KEY idx_material_lab_id (lab_id),
    KEY idx_material_name (material_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物资表';

-- 2. 物资批次表
CREATE TABLE material_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次编号，全局唯一',
    material_id BIGINT UNSIGNED NOT NULL COMMENT '关联物资ID，逻辑外键→material.id',
    quantity INT UNSIGNED NOT NULL COMMENT '入库数量',
    remaining INT UNSIGNED NOT NULL COMMENT '该批次在库余量',
    expiry_date DATE NOT NULL COMMENT '有效期至',
    stock_in_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入库时间',
    batch_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '批次状态：1在库可用 2已清空 3报废',
    operator_id BIGINT UNSIGNED NOT NULL COMMENT '入库操作人ID（person_id）',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_batch_material_id (material_id),
    KEY idx_batch_expiry_date (expiry_date),
    KEY idx_batch_status (batch_status),
    CONSTRAINT ck_material_batch_status CHECK (batch_status IN (1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物资批次表';

-- 3. 入库记录表
CREATE TABLE material_stock_in (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    stock_in_no VARCHAR(64) NOT NULL COMMENT '入库单号，全局唯一',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '关联批次ID，逻辑外键→material_batch.id',
    material_id BIGINT UNSIGNED NOT NULL COMMENT '关联物资ID，逻辑外键→material.id',
    quantity INT UNSIGNED NOT NULL COMMENT '入库数量',
    operator_id BIGINT UNSIGNED NOT NULL COMMENT '入库操作人ID（person_id）',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_in_no (stock_in_no),
    KEY idx_stock_in_batch_id (batch_id),
    KEY idx_stock_in_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库记录表';

-- 4. 领用申请表
CREATE TABLE material_receive_apply (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_no VARCHAR(64) NOT NULL COMMENT '申请单号，全局唯一',
    material_id BIGINT UNSIGNED NOT NULL COMMENT '物资ID，逻辑外键→material.id',
    applicant_id BIGINT UNSIGNED NOT NULL COMMENT '申请人ID（person_id）',
    approver_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人ID（person_id），审批时填入',
    expected_quantity INT UNSIGNED NOT NULL COMMENT '申请领用数量',
    purpose VARCHAR(255) DEFAULT NULL COMMENT '物资用途',
    apply_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '申请状态：1待审核 2审批通过 3驳回 4已取消',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '驳回原因，驳回时必填',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_apply_no (apply_no),
    KEY idx_apply_material_id (material_id),
    KEY idx_apply_applicant_id (applicant_id),
    KEY idx_apply_status (apply_status),
    CONSTRAINT ck_material_receive_apply_status CHECK (apply_status IN (1, 2, 3, 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领用申请表';

-- 5. 领用出库日志表
CREATE TABLE material_receive_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_id BIGINT UNSIGNED NOT NULL COMMENT '关联申请单ID，逻辑外键→material_receive_apply.id',
    material_id BIGINT UNSIGNED NOT NULL COMMENT '物资ID，逻辑外键→material.id',
    total_quantity INT UNSIGNED NOT NULL COMMENT '本次领用总数量',
    receiver_id BIGINT UNSIGNED NOT NULL COMMENT '领用人ID（person_id）',
    out_operator_id BIGINT UNSIGNED NOT NULL COMMENT '出库操作人ID（person_id）',
    out_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '出库时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_receive_log_apply_id (apply_id),
    KEY idx_receive_log_material_id (material_id),
    KEY idx_receive_log_receiver_id (receiver_id),
    KEY idx_receive_log_out_time (out_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='领用出库日志表';

-- 6. 出库批次扣减明细表
CREATE TABLE material_receive_batch_detail (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    receive_log_id BIGINT UNSIGNED NOT NULL COMMENT '关联出库日志ID',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '被扣减的批次ID',
    deducted_quantity INT UNSIGNED NOT NULL COMMENT '本批次扣减数量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_detail_receive_log_id (receive_log_id),
    KEY idx_detail_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库批次扣减明细表';

-- 7. 预警记录表
CREATE TABLE material_alert_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    material_id BIGINT UNSIGNED NOT NULL COMMENT '物资ID',
    batch_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联批次ID（低库存预警时为NULL）',
    alert_type TINYINT UNSIGNED NOT NULL COMMENT '预警类型：1低库存 2临期 3过期 4超量领用',
    alert_content VARCHAR(512) NOT NULL COMMENT '预警内容',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1已处理',
    handled_by BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID（person_id）',
    alert_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '预警触发时间',
    handled_at DATETIME(3) DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_alert_material_id (material_id),
    KEY idx_alert_type (alert_type),
    KEY idx_alert_status (status),
    KEY idx_alert_time (alert_time),
    CONSTRAINT ck_material_alert_record_type CHECK (alert_type IN (1, 2, 3, 4)),
    CONSTRAINT ck_material_alert_record_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警记录表';
