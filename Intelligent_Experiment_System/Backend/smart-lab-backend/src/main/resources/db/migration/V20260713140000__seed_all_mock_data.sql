-- ============================================================
-- 智慧实验室 - 全量测试数据种子脚本
-- 将前端所有 mock 数据写入数据库，实现前后端真实数据互通
-- ============================================================

-- 一、组织架构：部门 + 岗位
INSERT IGNORE INTO department (id, department_no, department_code, department_name, parent_id, department_status, sort_no, created_at, updated_at) VALUES
(1, 'DN-001', 'DEPT-001', '计算机科学与技术学院', NULL, 1, 1, NOW(), NOW()),
(2, 'DN-002', 'DEPT-002', '化学与材料学院', NULL, 1, 2, NOW(), NOW()),
(3, 'DN-003', 'DEPT-003', '物理与光电学院', NULL, 1, 3, NOW(), NOW()),
(4, 'DN-004', 'DEPT-004', '生物科学与工程学院', NULL, 1, 4, NOW(), NOW()),
(5, 'DN-005', 'DEPT-005', '实验室管理中心', NULL, 1, 5, NOW(), NOW()),
(6, 'DN-006', 'DEPT-006', '环境科学与工程学院', NULL, 1, 6, NOW(), NOW());

INSERT IGNORE INTO position (id, position_no, position_code, position_name, position_type, department_id, position_status, sort_no, created_at, updated_at) VALUES
(1, 'PN-001', 'POS-001', '本科生', 1, 1, 1, 1, NOW(), NOW()),
(2, 'PN-002', 'POS-002', '硕士研究生', 1, 1, 1, 2, NOW(), NOW()),
(3, 'PN-003', 'POS-003', '副教授', 2, 2, 1, 3, NOW(), NOW()),
(4, 'PN-004', 'POS-004', '教授', 2, 3, 1, 4, NOW(), NOW()),
(5, 'PN-005', 'POS-005', '高级实验师', 3, 5, 1, 5, NOW(), NOW()),
(6, 'PN-006', 'POS-006', '讲师', 2, 2, 1, 6, NOW(), NOW());

-- 二、人员档案 person
INSERT IGNORE INTO person (id, person_no, person_name, person_type, gender_type, phone, email, department_id, position_id, person_status, created_at, updated_at) VALUES
(1, 'P-20240001', '张同学', 1, 1, '13900001234', 'zhang@stu.edu.cn', 1, 1, 1, NOW(), NOW()),
(2, 'P-20240002', '王同学', 1, 1, '13900001235', 'wang2@stu.edu.cn', 1, 1, 1, NOW(), NOW()),
(3, 'P-20240003', '赵同学', 1, 2, '13900001236', 'zhao@stu.edu.cn', 1, 2, 1, NOW(), NOW()),
(4, 'P-20240004', '李老师', 2, 2, '13600005678', 'li@lab.edu.cn', 2, 3, 1, NOW(), NOW()),
(5, 'P-20240005', '孙老师', 2, 2, '13600005679', 'sun@lab.edu.cn', 3, 4, 1, NOW(), NOW()),
(6, 'P-20240006', '周老师', 2, 2, '13600005680', 'zhou@lab.edu.cn', 1, 6, 1, NOW(), NOW()),
(7, 'P-20240007', '王管理员', 3, 1, '13800009012', 'wang@lab.edu.cn', 5, 5, 1, NOW(), NOW()),
(8, 'P-20240008', '陈同学', 1, 1, '13900001238', 'chen@stu.edu.cn', 4, 1, 1, NOW(), NOW()),
(9, 'P-20240009', '刘同学', 1, 1, '13900001239', 'liu@stu.edu.cn', 3, 1, 1, NOW(), NOW());

-- 三、补充 user_profile 信息
UPDATE user_profile SET student_no = '2024001', department_name = '计算机科学与技术学院', class_name = '计科2401班' WHERE user_id = (SELECT id FROM user_account WHERE username = 'student001' LIMIT 1);
UPDATE user_profile SET employee_no = 'T2024001', department_name = '化学与材料学院', title_name = '副教授' WHERE user_id = (SELECT id FROM user_account WHERE username = 'teacher001' LIMIT 1);
UPDATE user_profile SET employee_no = 'A2024001', department_name = '实验室管理中心', title_name = '高级实验师' WHERE user_id = (SELECT id FROM user_account WHERE username = 'admin001' LIMIT 1);

-- 四、实验室场地
INSERT IGNORE INTO lab_type (id, lab_type_name, created_at, updated_at) VALUES
(1, '化学实验室', NOW(), NOW()), (2, '物理实验室', NOW(), NOW()),
(3, '计算机实验室', NOW(), NOW()), (4, '生物实验室', NOW(), NOW()), (5, '综合实验室', NOW(), NOW());

INSERT IGNORE INTO lab (id, lab_name, lab_type_id, campus, building, floor, room_no, capacity, area_m2, duty_user_id, running_status, description, created_at, updated_at) VALUES
(1, 'A305 分析化学实验室', 1, '主校区', '化学楼', 3, '305', 20, 90, 4, 1, '精密分析化学实验室', NOW(), NOW()),
(2, 'B103 物理实验室', 2, '主校区', '理工楼', 1, '103', 40, 120, 5, 1, '大学物理实验器材齐全', NOW(), NOW()),
(3, 'C202 计算机实验室', 3, '主校区', '信息楼', 2, '202', 60, 100, 6, 1, '60台工作站', NOW(), NOW()),
(4, 'D108 生物培养实验室', 4, '主校区', '生科楼', 1, '108', 25, 130, 4, 1, '分子生物学实验室', NOW(), NOW()),
(5, 'A201 有机化学实验室', 1, '主校区', '化学楼', 2, '201', 30, 150, 4, 1, '标准有机化学实验室', NOW(), NOW()),
(6, 'G102 环境监测实验室', 4, '南校区', '环境楼', 1, '102', 20, 100, 4, 1, '环境监测实验室', NOW(), NOW());

INSERT IGNORE INTO lab_detection_indicator (id, lab_type_id, indicator_name, unit, safe_range, created_at, updated_at) VALUES
(1, 1, 'temperature', '°C', '[[0,35]]', NOW(), NOW()),
(2, 1, 'humidity', '%', '[[30,70]]', NOW(), NOW()),
(3, 1, 'gas', 'ppm', '[[0,0.5]]', NOW(), NOW());

-- 五、设备 (device_status: 1=闲置 2=借用中 3=维修中 4=报废 5=采购待入库)
INSERT IGNORE INTO equipment_type (id, type_name, created_at, updated_at) VALUES
(1, '分析仪器', NOW(), NOW()), (2, '物理测量', NOW(), NOW()),
(3, '生物培养', NOW(), NOW()), (4, '计算机设备', NOW(), NOW()), (5, '通用设备', NOW(), NOW());

INSERT IGNORE INTO device (id, device_no, name, device_type_id, lab_id, purchase_date, purchase_price, expire_date, device_status, health_score, remark, created_at, updated_at) VALUES
(1, 'EQ-2024-001', '高速离心机', 1, 1, '2024-01-15', 85000.00, '2028-01-15', 1, 92, 'Eppendorf 5424R', NOW(), NOW()),
(2, 'EQ-2024-002', '数字示波器', 2, 2, '2024-03-20', 32000.00, '2028-03-20', 2, 78, 'Tektronix TBS1052C', NOW(), NOW()),
(3, 'EQ-2023-015', '恒温培养箱', 3, 4, '2023-06-10', 45000.00, '2027-06-10', 3, 55, '上海一恒 DHP-9162', NOW(), NOW()),
(4, 'EQ-2022-008', '分光光度计', 1, 2, '2022-11-01', 120000.00, '2026-11-01', 4, 12, '岛津 UV-2600i', NOW(), NOW()),
(5, 'EQ-2024-010', 'PCR扩增仪', 3, 4, '2024-05-08', 68000.00, '2028-05-08', 1, 96, 'Bio-Rad T100', NOW(), NOW()),
(6, 'EQ-2024-003', '电子天平', 1, 1, '2024-02-28', 15000.00, '2028-02-28', 5, 100, 'Mettler ME204', NOW(), NOW()),
(7, 'EQ-2023-020', '服务器机柜', 4, 3, '2023-09-12', 25000.00, '2027-09-12', 1, 85, '华为 FusionServer', NOW(), NOW()),
(8, 'EQ-2022-005', '显微镜', 2, 1, '2022-08-01', 38000.00, '2026-08-01', 1, 63, 'Olympus CX43', NOW(), NOW());

-- 六、设备借用申请 (apply_status: 1=待审批 2=已通过 3=已驳回 4=已取消)
INSERT IGNORE INTO device_borrow_apply (id, apply_no, device_id, applicant_id, approver_id, expected_start_time, expected_return_time, purpose, apply_status, reject_reason, created_at, updated_at) VALUES
(1, 'JY-DEV-001', 2, 1, NULL, '2026-07-09 09:00:00', '2026-07-10 18:00:00', '实验测量', 1, NULL, '2026-07-08 10:30:00', NOW()),
(2, 'JY-DEV-002', 1, 4, 7, '2026-07-12 08:00:00', '2026-07-14 17:00:00', '教学演示', 2, NULL, '2026-07-08 14:20:00', NOW()),
(3, 'JY-DEV-003', 4, 2, NULL, '2026-07-10 08:00:00', '2026-07-12 17:00:00', '科研测试', 1, NULL, '2026-07-08 09:00:00', NOW()),
(4, 'JY-DEV-004', 5, 5, 7, '2026-07-15 13:00:00', '2026-07-16 17:00:00', '实验教学', 2, NULL, '2026-07-07 08:00:00', NOW()),
(5, 'JY-DEV-005', 6, 3, 7, '2026-07-11 09:00:00', '2026-07-11 12:00:00', '称量实验', 3, '设备正在维修中，预计7月15日恢复使用', '2026-07-07 11:00:00', NOW()),
(6, 'JY-DEV-006', 8, 8, NULL, '2026-07-08 14:00:00', '2026-07-08 18:00:00', '观察实验', 4, NULL, '2026-07-06 10:00:00', NOW()),
(7, 'JY-DEV-007', 7, 6, 7, '2026-07-18 08:00:00', '2026-07-20 18:00:00', '课程需要', 2, NULL, '2026-07-06 16:00:00', NOW()),
(8, 'JY-DEV-008', 1, 9, 7, '2026-07-16 09:00:00', '2026-07-16 17:00:00', '实验', 3, '该时段已被其他教师预约占用', '2026-07-05 14:00:00', NOW());

-- 七、设备维修记录 (repair_status: 1=待处理 2=维修中 3=已完成 4=已验收)
INSERT IGNORE INTO device_repair_record (id, repair_no, device_id, reporter_id, handler_id, report_time, fault_desc, repair_status, created_at, updated_at) VALUES
(1, 'WX-2024-001', 3, 5, NULL, '2026-07-08 09:30:00', '温控异常，显示温度与实际温度偏差超过±2°C', 1, NOW(), NOW()),
(2, 'WX-2024-002', 8, 3, NULL, '2026-07-08 14:00:00', '物镜转换器卡滞，无法顺畅切换不同倍数物镜', 1, NOW(), NOW()),
(3, 'WX-2024-003', 4, 4, 7, '2026-07-07 10:00:00', '光源老化严重，波长校准异常', 2, NOW(), NOW()),
(4, 'WX-2024-004', 1, 2, 7, '2026-07-06 16:00:00', '运行时异响，转速不稳定', 2, NOW(), NOW()),
(5, 'WX-2024-005', 7, 6, 7, '2026-07-05 08:00:00', '散热风扇故障，机柜内温度过高报警', 3, NOW(), NOW()),
(6, 'WX-2024-006', 2, 1, 7, '2026-07-04 11:00:00', '屏幕显示异常，出现竖条纹', 4, NOW(), NOW());

-- 八、实验室预约 (reservation_status: 1=待审批 2=已通过 3=已驳回 4=已取消)
INSERT IGNORE INTO lab_reservation (id, reservation_no, lab_id, applicant_id, reservation_date, start_time, end_time, purpose, reservation_status, approver_id, cancel_reason, created_at, updated_at) VALUES
(1, 'LAB-RSV-001', 2, 4, '2026-07-12', '09:00:00', '12:00:00', '物理实验课程教学', 1, NULL, NULL, '2026-07-08 14:30:00', NOW()),
(2, 'LAB-RSV-002', 3, 2, '2026-07-13', '14:00:00', '17:00:00', '算法竞赛集训', 1, NULL, NULL, '2026-07-08 16:00:00', NOW()),
(3, 'LAB-RSV-003', 1, 1, '2026-07-11', '13:00:00', '16:00:00', '课题组实验', 3, 7, '该时段已被占用', '2026-07-06 16:00:00', NOW()),
(4, 'LAB-RSV-004', 4, 6, '2026-07-15', '08:00:00', '11:00:00', '生物实验课程', 1, NULL, NULL, '2026-07-08 11:00:00', NOW()),
(5, 'LAB-RSV-005', 6, 5, '2026-07-18', '08:00:00', '12:00:00', '环境采样分析', 2, 7, NULL, '2026-07-07 08:00:00', NOW()),
(6, 'LAB-RSV-006', 3, 3, '2026-07-10', '09:00:00', '11:00:00', '毕业设计答辩', 4, NULL, NULL, '2026-07-05 09:00:00', NOW());

-- 九、耗材
INSERT IGNORE INTO material (id, material_no, material_name, lab_id, unit, safety_stock, current_stock, created_at, updated_at) VALUES
(1, 'CON-001', '无水乙醇', 1, '瓶', 20, 45, NOW(), NOW()),
(2, 'CON-002', '硫酸铜', 1, '瓶', 10, 5, NOW(), NOW()),
(3, 'CON-003', '烧杯', 1, '个', 50, 120, NOW(), NOW()),
(4, 'CON-004', '丁腈手套', 1, '盒', 30, 200, NOW(), NOW()),
(5, 'CON-005', 'pH试纸', 1, '盒', 20, 60, NOW(), NOW()),
(6, 'CON-006', '定量滤纸', 1, '盒', 15, 3, NOW(), NOW()),
(7, 'CON-007', '琼脂粉', 4, '瓶', 10, 8, NOW(), NOW()),
(8, 'CON-008', '试管刷', 1, '个', 20, 35, NOW(), NOW());

-- batch_status: 1=在库可用 2=已用完 3=已过期
INSERT IGNORE INTO material_batch (id, batch_no, material_id, quantity, remaining, expiry_date, stock_in_time, batch_status, operator_id, created_at, updated_at) VALUES
(1, 'LOT-20260601', 1, 25, 25, '2028-06-01', '2026-06-01 08:00:00', 1, 7, NOW(), NOW()),
(2, 'LOT-20260315', 1, 20, 20, '2028-03-15', '2026-03-15 08:00:00', 1, 7, NOW(), NOW()),
(3, 'LOT-20260520', 2, 3, 3, '2028-05-20', '2026-05-20 08:00:00', 1, 7, NOW(), NOW()),
(4, 'LOT-20260701', 3, 120, 120, '2031-07-01', '2026-07-01 08:00:00', 1, 7, NOW(), NOW()),
(5, 'LOT-20260705', 4, 150, 150, '2029-07-05', '2026-07-05 08:00:00', 1, 7, NOW(), NOW()),
(6, 'LOT-20260401', 6, 3, 3, '2027-04-01', '2026-04-01 08:00:00', 1, 7, NOW(), NOW());

-- apply_status: 1=待审批 2=已通过 3=已驳回
INSERT IGNORE INTO material_receive_apply (id, apply_no, material_id, applicant_id, approver_id, expected_quantity, purpose, apply_status, reject_reason, created_at, updated_at) VALUES
(1, 'PICK-2024-001', 1, 1, NULL, 2, '有机化学实验-萃取操作', 1, NULL, '2026-07-08 14:30:00', NOW()),
(2, 'PICK-2024-002', 3, 4, NULL, 5, '物理实验课程演示', 1, NULL, '2026-07-08 16:00:00', NOW()),
(3, 'PICK-2024-003', 4, 2, 7, 10, '生物实验防护使用', 3, '当前库存不足，仅剩8盒', '2026-07-07 10:00:00', NOW()),
(4, 'PICK-2024-004', 2, 5, NULL, 1, '分析化学实验', 1, NULL, '2026-07-09 09:00:00', NOW()),
(5, 'PICK-2024-005', 5, 3, 7, 3, '水质检测实验', 2, NULL, '2026-07-06 11:00:00', NOW());

-- alert_type: 1=低库存 2=临期 3=过期; status: 0=待处理 1=已处理
INSERT IGNORE INTO material_alert_record (id, material_id, batch_id, alert_type, alert_content, status, alert_time, created_at, updated_at) VALUES
(1, 1, NULL, 1, '无水乙醇当前库存45瓶，安全库存50瓶（偏低预警）', 0, '2026-07-10 08:00:00', NOW(), NOW()),
(2, 6, 6, 1, '定量滤纸当前库存3盒，安全库存15盒（严重不足）', 0, '2026-07-10 08:00:00', NOW(), NOW()),
(3, 7, NULL, 1, '琼脂粉当前库存8瓶，安全库存10瓶', 0, '2026-07-10 08:00:00', NOW(), NOW()),
(4, 2, 3, 2, '硫酸铜批次LOT-20260520剩余3瓶，临近过期', 0, '2026-07-10 08:00:00', NOW(), NOW());

-- 十、智能化教学 (course_status: 1=进行中 2=已结束)
INSERT IGNORE INTO course (id, course_code, course_name, semester, credit, total_hours, teacher_id, description, course_status, created_at, updated_at) VALUES
(1, 'COURSE-2024-001', '有机化学实验', '2026-2027-1', 3, 48, 4, '掌握有机化学基本实验操作技能', 1, NOW(), NOW()),
(2, 'COURSE-2024-002', '大学物理实验', '2026-2027-1', 3, 48, 5, '涵盖力学、电磁学、光学等基础物理实验', 1, NOW(), NOW()),
(3, 'COURSE-2024-003', '生物培养技术', '2026-2027-1', 2, 32, 6, '学习细胞培养与无菌操作技术', 1, NOW(), NOW()),
(4, 'COURSE-2024-004', 'Python程序设计', '2026-2027-1', 2, 32, 4, 'Python编程与数据分析实践', 1, NOW(), NOW()),
(5, 'COURSE-2024-005', '无机化学实验', '2026-2026-2', 3, 48, 4, '已完成', 2, NOW(), NOW()),
(6, 'COURSE-2024-006', '数据分析综合实验', '2026-2027-1', 2, 32, NULL, '新课筹备中', 1, NOW(), NOW());

INSERT IGNORE INTO course_lab (id, course_id, lab_id, bind_status, created_at, updated_at) VALUES
(1, 1, 5, 1, NOW(), NOW()), (2, 2, 2, 1, NOW(), NOW()),
(3, 3, 4, 1, NOW(), NOW()), (4, 4, 3, 1, NOW(), NOW());

INSERT IGNORE INTO course_student (id, course_id, student_id, enroll_status, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), NOW()), (2, 1, 2, 1, NOW(), NOW()), (3, 1, 3, 1, NOW(), NOW()),
(4, 2, 1, 1, NOW(), NOW()), (5, 3, 1, 1, NOW(), NOW()), (6, 3, 8, 1, NOW(), NOW());

-- task_status: 1=草稿 2=已发布; submit_type: 1=文本 2=文件 3=文本+文件
INSERT IGNORE INTO teaching_task (id, task_no, course_id, task_title, task_content, submit_type, deadline, task_weight, task_status, created_by, created_at, updated_at) VALUES
(1, 'TASK-001', 1, '酯化反应实验报告', '完成乙酸乙酯的制备实验，记录反应条件和产率，撰写完整的实验报告。', 3, '2026-07-12 23:59:00', 40.00, 2, 4, NOW(), NOW()),
(2, 'TASK-002', 1, '蒸馏与沸点测定', '掌握常压蒸馏和分馏的基本操作，测定不同有机溶剂的沸点。', 3, '2026-07-20 23:59:00', 30.00, 2, 4, NOW(), NOW()),
(3, 'TASK-003', 2, '力学基本测量', '掌握游标卡尺、螺旋测微计等基本测量工具的使用方法。', 3, '2026-07-14 23:59:00', 50.00, 2, 5, NOW(), NOW()),
(4, 'TASK-004', 4, 'Python 数据分析基础', '学习使用Python进行数据分析的基本流程，使用NumPy、Pandas、Matplotlib库。', 3, '2026-07-18 23:59:00', 50.00, 2, 4, NOW(), NOW()),
(5, 'TASK-005', 1, '实验安全预习报告', '查阅化学品安全技术说明书（MSDS），完成预习报告。', 1, '2026-07-08 12:00:00', 30.00, 2, 4, NOW(), NOW()),
(6, 'TASK-006', 2, '电磁学实验', '掌握示波器的使用方法，测量交流信号的电压、频率和相位。', 3, '2026-07-22 23:59:00', 50.00, 2, 5, NOW(), NOW());

-- 十一、消息模板 (status: 1=启用 0=停用)
INSERT IGNORE INTO msg_template (id, template_code, template_name, message_type, title_template, content_template, content_format, status, created_at, updated_at) VALUES
(1, 'equip_loan_approved', '设备借用审批通过', 'equipment.loan.approved', '您的设备借用申请已通过审批', '您好{user}：\n您对{device}的借用申请已审批通过。', 'TEXT', 1, NOW(), NOW()),
(2, 'equip_loan_rejected', '设备借用审批驳回', 'equipment.loan.rejected', '您的设备借用申请已被驳回', '您好{user}：\n您对{device}的借用申请未通过审批。\n原因：{reason}', 'TEXT', 1, NOW(), NOW()),
(3, 'lab_reserve_approved', '实验室预约通过', 'lab.reserve.approved', '您的实验室预约已通过审批', '您好{user}：\n您预约的{lab}已审批通过。', 'TEXT', 1, NOW(), NOW()),
(4, 'safety_alarm', '安全告警通知', 'safety.warning.alert', '安全告警通知', '实验室：{lab}\n告警对象：{alarm_object}\n告警值：{alarm_value}（阈值：{threshold}）', 'TEXT', 1, NOW(), NOW()),
(5, 'duty_remind', '值日提醒通知', 'duty.normal.remind', '值日提醒通知', '{user}同学：\n提醒您于{date} {time_slot}在{lab}进行值日。', 'TEXT', 1, NOW(), NOW());

-- 十二、传感器与物联网 (running_status: 1=运行中 2=离线)
INSERT IGNORE INTO sensor_type (id, sensor_type_name, created_at, updated_at) VALUES
(1, '温湿度传感器', NOW(), NOW()), (2, '烟雾传感器', NOW(), NOW()), (3, '气体传感器', NOW(), NOW());

INSERT IGNORE INTO sensor_data_type (id, sensor_type_id, data_name, unit, created_at, updated_at) VALUES
(1, 1, 'temperature', '°C', NOW(), NOW()), (2, 1, 'humidity', '%', NOW(), NOW()),
(3, 2, 'smoke', 'mg/m³', NOW(), NOW()), (4, 3, 'gas', 'ppm', NOW(), NOW());

INSERT IGNORE INTO sensor (id, sensor_no, sensor_type_id, target_type, target_id, running_status, last_report_at, created_at, updated_at) VALUES
(1, 'SEN-001', 1, 2, 1, 1, '2026-07-11 12:00:00', NOW(), NOW()),
(2, 'SEN-002', 2, 2, 2, 1, '2026-07-11 12:00:00', NOW(), NOW()),
(3, 'SEN-003', 3, 2, 3, 1, '2026-07-11 12:00:00', NOW(), NOW()),
(4, 'SEN-004', 1, 2, 4, 1, '2026-07-11 12:00:00', NOW(), NOW()),
(5, 'SEN-005', 2, 1, 1, 2, NULL, NOW(), NOW()),
(6, 'SEN-006', 3, 1, 2, 2, NULL, NOW(), NOW());
