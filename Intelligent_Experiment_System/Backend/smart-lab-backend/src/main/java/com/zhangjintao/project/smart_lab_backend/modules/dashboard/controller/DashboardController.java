package com.zhangjintao.project.smart_lab_backend.modules.dashboard.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 顶部统计卡片
        stats.put("totalPersons", count("person", "deleted_at IS NULL"));
        stats.put("activeDevices", count("device", "device_status = 1"));
        stats.put("pendingApprovals",
                count("device_borrow_apply", "apply_status = 1") +
                count("lab_reservation", "reservation_status = 1"));
        stats.put("alarms", count("material_alert_record", "status = 0"));

        // 系统概览
        stats.put("studentCount", count("person", "person_type = 1 AND deleted_at IS NULL"));
        stats.put("teacherCount", count("person", "person_type = 2 AND deleted_at IS NULL"));
        stats.put("adminCount", count("person", "person_type = 3 AND deleted_at IS NULL"));
        stats.put("departmentCount", count("department", "deleted_at IS NULL"));
        stats.put("labCount", count("lab", "deleted_at IS NULL"));
        stats.put("deviceCount", count("device", null));

        // 本月借用/预约
        stats.put("monthBorrowCount", count("device_borrow_apply",
                "YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())"));
        stats.put("monthReservationCount", count("lab_reservation",
                "YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())"));

        // 待处理事项（取最新7条）
        stats.put("pendingItems", jdbcTemplate.queryForList(
                "SELECT * FROM (" +
                "  SELECT 'borrow' AS type, CONCAT('借用申请: ', purpose) AS title, created_at AS time, 'warn' AS level, 'device-approve' AS navKey, '借用审批' AS navName FROM device_borrow_apply WHERE apply_status = 1 " +
                "  UNION ALL " +
                "  SELECT 'reservation' AS type, CONCAT('预约申请: ', purpose) AS title, created_at AS time, 'warn' AS level, 'lab-approve' AS navKey, '预约审核' AS navName FROM lab_reservation WHERE reservation_status = 1 " +
                "  UNION ALL " +
                "  SELECT 'repair' AS type, CONCAT('维修: ', fault_desc) AS title, report_time AS time, 'info' AS level, 'device-repair' AS navKey, '维修管理' AS navName FROM device_repair_record WHERE repair_status = 1 " +
                "  UNION ALL " +
                "  SELECT 'alert' AS type, alert_content AS title, alert_time AS time, 'alarm' AS level, 'consumable-warn' AS navKey, '库存预警' AS navName FROM material_alert_record WHERE status = 0 " +
                ") t ORDER BY t.time DESC LIMIT 7"));

        // ===== 效能看板数据 =====
        // 实验室设备分布
        stats.put("labUsageList", jdbcTemplate.queryForList(
                "SELECT l.lab_name AS name, COUNT(d.id) AS count FROM lab l LEFT JOIN device d ON d.lab_id = l.id AND l.deleted_at IS NULL GROUP BY l.id, l.lab_name ORDER BY count DESC"));

        // 设备状态分布
        stats.put("deviceStatusBreakdown", jdbcTemplate.queryForList(
                "SELECT device_status AS status, COUNT(*) AS count FROM device GROUP BY device_status"));

        // 月度借用趋势（近6个月）
        stats.put("monthlyBorrowTrend", jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count FROM device_borrow_apply " +
                "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month"));

        // 月度预约趋势
        stats.put("monthlyReservationTrend", jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count FROM lab_reservation " +
                "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month"));

        // 总设备/借用/维修统计
        stats.put("totalDeviceCount", count("device", null));
        stats.put("borrowedDeviceCount", count("device", "device_status = 3"));
        stats.put("repairDeviceCount", count("device", "device_status = 4"));
        stats.put("totalRepairCount", count("device_repair_record", null));

        return ApiResult.success(stats);
    }

    private long count(String table, String where) {
        String sql = "SELECT COUNT(*) FROM " + table;
        if (where != null && !where.isEmpty()) {
            sql += " WHERE " + where;
        }
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0;
    }
}
