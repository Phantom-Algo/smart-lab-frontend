package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 收件箱消息列表项响应。
 */
@Value
@Builder
public class InboxMessageListItemResponse {

	Long inboxMessageId;
	Long messageId;
	String title;
	Integer priority;
	String priorityDescription;
	String messageType;
	LocalDateTime sentAt;
	LocalDateTime receivedAt;
	boolean read;
	LocalDateTime readAt;

	public static InboxMessageListItemResponse from(InboxMessageQueryDTO inboxMessage) {
		MessagePriority priority = MessagePriority.fromCode(inboxMessage.getPriority());
		return InboxMessageListItemResponse.builder()
				.inboxMessageId(inboxMessage.getInboxMessageId())
				.messageId(inboxMessage.getMessageId())
				.title(inboxMessage.getTitle())
				.priority(priority.getCode())
				.priorityDescription(priority.getDescription())
				.messageType(inboxMessage.getMessageType())
				.sentAt(inboxMessage.getSentAt())
				.receivedAt(inboxMessage.getReceivedAt())
				.read(inboxMessage.isRead())
				.readAt(inboxMessage.getReadAt())
				.build();
	}
}
