package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收件箱消息查询投影。
 */
@Data
public class InboxMessageQueryDTO {

	private Long inboxMessageId;

	private Long messageId;

	private String title;

	private Integer priority;

	private String messageType;

	private LocalDateTime sentAt;

	private LocalDateTime receivedAt;

	private Integer readFlag;

	private LocalDateTime readAt;

	private String content;

	private String contentFormat;

	private String actionUrl;

	public boolean isRead() {
		return readFlag != null && readFlag == 1;
	}
}
