package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 为本地演示账号补齐真实收件箱数据，避免真实模式下消息中心空白。
 */
@Slf4j
@Component
@DependsOn("testDataInitializer")
@RequiredArgsConstructor
public class TestInboxDataInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void initDemoInboxMessages() {
        seedForUser("student001", List.of(
                message("system-welcome", "system.normal.manual", "系统通知", "欢迎使用智慧实验室平台",
                        "你的账号已完成初始化，可以在平台内查看课程、预约实验室和借用设备。", 2, false),
                message("reservation-rejected", "lab_management.system.reservation_rejected", "预约消息", "实验室预约已驳回",
                        "你的实验室预约申请未通过。\n实验室：A305 分析化学实验室\n原因：该时段已有课程安排。", 3, false),
                message("borrow-approved", "equipment.normal.borrow.approved", "借用通知", "设备借用申请已通过",
                        "你的设备借用申请已审核通过。\n设备：高速离心机\n请按预约时间前往实验室领取。", 3, true)
        ));
        seedForUser("teacher001", List.of(
                message("teaching-task", "teaching.task.published", "教学通知", "新教学任务已发布",
                        "课程《有机化学实验》已发布新任务，请及时查看学生提交情况。", 3, false),
                message("reservation-approved", "lab_management.system.reservation_approved", "预约消息", "实验室预约已通过",
                        "你的实验室预约申请已审核通过。\n实验室：B103 物理实验室\n请按时使用。", 3, false),
                message("material-rejected", "material.normal.receive.rejected", "耗材通知", "耗材领用申请已驳回",
                        "你的耗材领用申请已被驳回。\n耗材：硫酸铜\n原因：当前库存不足。", 3, true)
        ));
        seedForUser("admin001", List.of(
                message("reservation-pending", "lab_management.system.reservation_pending_review", "审批消息", "新的实验室预约待审核",
                        "有新的实验室预约申请需要处理。\n申请人：李老师\n实验室：B103 物理实验室。", 2, false),
                message("equipment-pending", "equipment.system.borrow.pending_review", "审批消息", "新的设备借用申请待审批",
                        "有新的设备借用申请需要处理。\n申请人：张同学\n设备：高速离心机。", 2, false),
                message("material-pending", "material.system.receive.pending_review", "审批消息", "新的耗材领用申请待审批",
                        "有新的耗材领用申请需要处理。\n申请人：张同学\n耗材：无水乙醇。", 2, false)
        ));
        log.info("演示收件箱消息初始化完成");
    }

    private void seedForUser(String username, List<DemoMessage> messages) {
        Long userId = findUserId(username);
        if (userId == null) {
            log.warn("演示账号不存在，跳过收件箱初始化，username={}", username);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (DemoMessage message : messages) {
            String eventNo = "demo-inbox-" + username + "-" + message.key;
            jdbcTemplate.update("""
                    INSERT IGNORE INTO msg_event
                    (event_no, source_system, message_type, priority, occurred_at, target_type, targets,
                     title, content, content_format, dispatch_status, processed_at, created_at, updated_at)
                    VALUES (?, 'demo', ?, ?, ?, 'single', JSON_ARRAY(?),
                            ?, ?, 'TEXT', 2, ?, ?, ?)
                    """, eventNo, message.messageType, message.priority, now, userId,
                    message.title, message.content, now, now, now);
            Long messageId = findMessageId(eventNo);
            if (messageId == null) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT IGNORE INTO user_inbox_message
                    (user_id, message_id, is_read, read_at, is_deleted, received_at)
                    VALUES (?, ?, ?, ?, 0, ?)
                    """, userId, messageId, message.read ? 1 : 0, message.read ? now : null, now);
        }
    }

    private Long findUserId(String username) {
        List<Long> ids = jdbcTemplate.query("SELECT id FROM user_account WHERE username = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), username);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findMessageId(String eventNo) {
        List<Long> ids = jdbcTemplate.query("SELECT id FROM msg_event WHERE event_no = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), eventNo);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static DemoMessage message(String key, String messageType, String tag, String title,
                                       String content, int priority, boolean read) {
        return new DemoMessage(key, messageType, tag, title, content, priority, read);
    }

    private static class DemoMessage {

        private final String key;
        private final String messageType;
        @SuppressWarnings("unused")
        private final String tag;
        private final String title;
        private final String content;
        private final int priority;
        private final boolean read;

        private DemoMessage(String key, String messageType, String tag, String title,
                            String content, int priority, boolean read) {
            this.key = key;
            this.messageType = messageType;
            this.tag = tag;
            this.title = title;
            this.content = content;
            this.priority = priority;
            this.read = read;
        }
    }
}
