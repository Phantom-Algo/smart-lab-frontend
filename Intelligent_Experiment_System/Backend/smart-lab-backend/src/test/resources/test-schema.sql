-- ============================================================
-- TEST SCHEMA — H2 MySQL 兼容模式
-- ============================================================

CREATE TABLE IF NOT EXISTS equipment_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS equipment_metric_indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_type_id BIGINT NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    unit VARCHAR(32),
    safe_scope VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_no VARCHAR(64) NOT NULL,
    name VARCHAR(128),
    device_type_id BIGINT,
    lab_id BIGINT,
    purchase_date DATE,
    purchase_price DECIMAL(12,2),
    expire_date DATE,
    device_status INT DEFAULT 2,
    health_score INT DEFAULT 100,
    version INT DEFAULT 0,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_borrow_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apply_no VARCHAR(32) NOT NULL,
    device_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT,
    expected_start_time TIMESTAMP,
    expected_return_time TIMESTAMP,
    purpose VARCHAR(255),
    apply_status INT DEFAULT 1,
    is_overdue INT DEFAULT 0,
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_borrow_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apply_id BIGINT,
    device_id BIGINT NOT NULL,
    borrower_id BIGINT NOT NULL,
    out_operator_id BIGINT,
    return_operator_id BIGINT,
    expected_return_time TIMESTAMP,
    actual_out_time TIMESTAMP,
    actual_return_time TIMESTAMP,
    status INT DEFAULT 1,
    is_overdue INT DEFAULT 0,
    return_remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_repair_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repair_no VARCHAR(32) NOT NULL,
    device_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    handler_id BIGINT,
    repair_status INT DEFAULT 1,
    fault_desc VARCHAR(512),
    repair_desc VARCHAR(512),
    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_repair_cost (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repair_id BIGINT NOT NULL,
    cost DECIMAL(10,2) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_data_monitor_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    sensor_id BIGINT,
    detected_data VARCHAR(1024),
    is_safe INT DEFAULT 1,
    detected_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device_alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    sensor_id BIGINT,
    param_name VARCHAR(64),
    actual_value DECIMAL(10,2),
    safe_scope VARCHAR(512),
    exceed_direction INT,
    alert_time TIMESTAMP,
    status INT DEFAULT 0,
    handler_id BIGINT,
    handle_result VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS person (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_no VARCHAR(64) NOT NULL,
    person_name VARCHAR(64) NOT NULL,
    person_type INT NOT NULL DEFAULT 1,
    gender_type INT DEFAULT NULL,
    phone VARCHAR(32) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    id_card_no_masked VARCHAR(32) DEFAULT NULL,
    student_no VARCHAR(64) DEFAULT NULL,
    employee_no VARCHAR(64) DEFAULT NULL,
    department_id BIGINT DEFAULT NULL,
    position_id BIGINT DEFAULT NULL,
    class_name VARCHAR(128) DEFAULT NULL,
    title_name VARCHAR(128) DEFAULT NULL,
    person_status INT NOT NULL DEFAULT 1,
    entry_at TIMESTAMP DEFAULT NULL,
    leave_at TIMESTAMP DEFAULT NULL,
    source_type INT NOT NULL DEFAULT 1,
    description VARCHAR(512) DEFAULT NULL,
    extra_data TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_no VARCHAR(64) NOT NULL,
    department_code VARCHAR(64),
    department_name VARCHAR(128) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    leader_person_id BIGINT DEFAULT NULL,
    department_status INT DEFAULT 1,
    sort_no INT DEFAULT 0,
    description VARCHAR(512) DEFAULT NULL,
    extra_data TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS "position" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    position_no VARCHAR(64) NOT NULL,
    position_name VARCHAR(128) NOT NULL,
    position_code VARCHAR(64),
    department_id BIGINT DEFAULT NULL,
    position_status INT DEFAULT 1,
    sort_no INT DEFAULT 0,
    description VARCHAR(512) DEFAULT NULL,
    extra_data TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    person_id BIGINT,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(64),
    user_type INT NOT NULL DEFAULT 1,
    account_status INT NOT NULL DEFAULT 1,
    failed_login_count INT DEFAULT 0,
    lock_expires_at TIMESTAMP,
    last_login_ip VARCHAR(64),
    last_login_at TIMESTAMP,
    last_password_changed_at TIMESTAMP,
    is_password_reset_required INT DEFAULT 0,
    extra_data TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS login_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255),
    login_ip VARCHAR(64),
    login_device VARCHAR(255),
    expires_at TIMESTAMP,
    is_logged_out INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 物资管理子系统 — 7张表（H2 MySQL兼容模式）
-- ============================================================

CREATE TABLE IF NOT EXISTS material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_no VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    lab_id BIGINT NOT NULL,
    unit VARCHAR(32) NOT NULL,
    safety_stock INT NOT NULL DEFAULT 0,
    current_stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    material_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    remaining INT NOT NULL,
    expiry_date DATE NOT NULL,
    stock_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    batch_status INT NOT NULL DEFAULT 1,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_stock_in (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_in_no VARCHAR(64) NOT NULL,
    batch_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_receive_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apply_no VARCHAR(64) NOT NULL,
    material_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    approver_id BIGINT,
    expected_quantity INT NOT NULL,
    purpose VARCHAR(255),
    apply_status INT NOT NULL DEFAULT 1,
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_receive_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apply_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    receiver_id BIGINT NOT NULL,
    out_operator_id BIGINT NOT NULL,
    out_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_receive_batch_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receive_log_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    deducted_quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS material_alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    batch_id BIGINT,
    alert_type INT NOT NULL,
    alert_content VARCHAR(512) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    handled_by BIGINT,
    alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- H2 不支持 ALTER TABLE ADD COLUMN ... GENERATED ALWAYS AS，在 CREATE 时直接定义
ALTER TABLE material_alert_record ADD COLUMN alert_day DATE;
-- 由于 H2 中 alert_day 没有 GENERATED ALWAYS，用触发器或应用层保证；唯一约束仍有效
CREATE UNIQUE INDEX IF NOT EXISTS uk_alert_material_batch_type_day ON material_alert_record(material_id, batch_id, alert_type, alert_day);
