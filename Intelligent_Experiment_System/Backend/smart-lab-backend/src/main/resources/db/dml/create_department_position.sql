-- ============================================================
-- 部门 & 岗位 模拟测试数据
-- 用于开发/测试环境初始化
-- ============================================================

-- ----------------------------
-- 1. 组织部门数据（9个部门，树形结构）
-- ----------------------------
INSERT IGNORE INTO `department` (`id`, `department_no`, `department_code`, `department_name`, `parent_id`, `leader_person_id`, `department_status`, `sort_no`, `description`) VALUES
(1,  'DEPT-001', 'SMART_LAB_CENTER',      '智慧实验室管理中心', NULL, NULL, 1, 1, '校级智慧实验室统一管理机构'),
(2,  'DEPT-002', 'SCHOOL_CS',              '计算机科学与技术学院', 1,    NULL, 1, 1, NULL),
(3,  'DEPT-003', 'DEPT_SOFTWARE_ENG',      '软件工程系',           2,    NULL, 1, 1, '软件工程专业教研室'),
(4,  'DEPT-004', 'DEPT_COMPUTER_SCI',      '计算机科学系',         2,    NULL, 1, 2, '计算机科学与技术专业教研室'),
(5,  'DEPT-005', 'SCHOOL_EE',              '电子信息工程学院',     1,    NULL, 1, 2, NULL),
(6,  'DEPT-006', 'DEPT_COMM_ENG',          '通信工程系',           5,    NULL, 1, 1, '通信工程专业教研室'),
(7,  'DEPT-007', 'DEPT_LAB_MGMT',          '实验室管理科',         1,    NULL, 1, 3, '负责实验室日常运营与维护'),
(8,  'DEPT-008', 'GROUP_EQUIP_MGMT',       '设备管理组',           7,    NULL, 1, 1, '负责实验设备采购、维护与报废'),
(9,  'DEPT-009', 'GROUP_SAFETY_MGMT',      '安全管理组',           7,    NULL, 1, 2, '负责实验室安全规范制定与检查');

-- ----------------------------
-- 2. 岗位数据（20个岗位，覆盖学生/教师/管理三种类型）
-- ----------------------------
INSERT IGNORE INTO `position` (`id`, `position_no`, `position_code`, `position_name`, `position_type`, `department_id`, `position_status`, `sort_no`, `description`) VALUES
-- 学生岗位 (position_type = 1) — 软件工程系
(1,  'POS-001', 'STU_UG_SE',        '本科生（软件工程）',   1, 3, 1, 1, '软件工程系在读本科生'),
(2,  'POS-002', 'STU_MS_SE',        '硕士研究生（软件工程）', 1, 3, 1, 2, '软件工程系在读硕士研究生'),
(3,  'POS-003', 'STU_PHD_SE',       '博士研究生（软件工程）', 1, 3, 1, 3, '软件工程系在读博士研究生'),

-- 学生岗位 (position_type = 1) — 计算机科学系
(4,  'POS-004', 'STU_UG_CS',        '本科生（计算机科学）',   1, 4, 1, 1, '计算机科学系在读本科生'),
(5,  'POS-005', 'STU_MS_CS',        '硕士研究生（计算机科学）', 1, 4, 1, 2, '计算机科学系在读硕士研究生'),

-- 学生岗位 (position_type = 1) — 通信工程系
(6,  'POS-006', 'STU_UG_CE',        '本科生（通信工程）',   1, 6, 1, 1, '通信工程系在读本科生'),

-- 教师岗位 (position_type = 2) — 软件工程系
(7,  'POS-007', 'TEACHER_PROF_SE',  '教授（软件工程）',     2, 3, 1, 1, '正高级职称'),
(8,  'POS-008', 'TEACHER_APROF_SE', '副教授（软件工程）',   2, 3, 1, 2, '副高级职称'),
(9,  'POS-009', 'TEACHER_LECT_SE',  '讲师（软件工程）',     2, 3, 1, 3, '中级职称'),
(10, 'POS-010', 'TEACHER_TA_SE',    '助教（软件工程）',     2, 3, 1, 4, '初级职称'),

-- 教师岗位 (position_type = 2) — 计算机科学系
(11, 'POS-011', 'TEACHER_PROF_CS',  '教授（计算机科学）',   2, 4, 1, 1, '正高级职称'),
(12, 'POS-012', 'TEACHER_APROF_CS', '副教授（计算机科学）', 2, 4, 1, 2, '副高级职称'),

-- 教师岗位 (position_type = 2) — 通信工程系
(13, 'POS-013', 'TEACHER_PROF_CE',  '教授（通信工程）',     2, 6, 1, 1, '正高级职称'),
(14, 'POS-014', 'TEACHER_LECT_CE',  '讲师（通信工程）',     2, 6, 1, 2, '中级职称'),

-- 管理岗位 (position_type = 3)
(15, 'POS-015', 'ADMIN_CENTER_DIR', '中心主任',             3, 1, 1, 1, '智慧实验室管理中心负责人'),
(16, 'POS-016', 'ADMIN_VICE_DEAN',  '副院长',               3, 2, 1, 1, '学院分管副院长'),
(17, 'POS-017', 'ADMIN_DEPT_HEAD',  '系主任',               3, 3, 1, 1, '软件工程系主任'),
(18, 'POS-018', 'ADMIN_LAB_MGR',    '实验室管理员',         3, 7, 1, 1, '负责实验室日常管理与排班'),
(19, 'POS-019', 'ADMIN_EQUIP_MGR',  '设备管理员',           3, 8, 1, 1, '负责设备采购、盘点与维护'),
(20, 'POS-020', 'ADMIN_SAFETY_OFF', '安全员',               3, 9, 1, 1, '负责实验室安全检查与培训');
