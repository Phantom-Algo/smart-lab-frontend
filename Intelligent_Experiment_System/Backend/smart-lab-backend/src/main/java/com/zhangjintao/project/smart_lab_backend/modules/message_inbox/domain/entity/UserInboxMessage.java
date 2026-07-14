package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收件箱消息领域实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInboxMessage {

	private Long id;

	private Long userId;

	private Long messageId;

	private boolean read;

	private LocalDateTime readAt;

	private boolean deleted;

	private LocalDateTime deletedAt;

	private LocalDateTime receivedAt;

	public static UserInboxMessage unread(Long messageId, Long userId, LocalDateTime receivedAt) {
		return UserInboxMessage.builder()
				.messageId(messageId)
				.userId(userId)
				.read(false)
				.deleted(false)
				.receivedAt(receivedAt)
				.build();
	}
}
