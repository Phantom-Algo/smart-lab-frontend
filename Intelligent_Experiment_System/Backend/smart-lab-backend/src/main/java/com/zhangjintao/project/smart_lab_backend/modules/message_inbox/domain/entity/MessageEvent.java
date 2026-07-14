package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DispatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageTypeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息事件领域实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {

	private Long id;

	private String eventNo;

	private String sourceSystem;

	private String messageType;

	private MessagePriority priority;

	private LocalDateTime occurredAt;

	private TargetType targetType;

	private List<Long> targetUserIds;

	private Long templateId;

	private TempMessageTemplate tempTemplate;

	private Map<String, Object> params;

	private String title;

	private String content;

	private ContentFormat contentFormat;

	private String actionUrl;

	private DispatchStatus dispatchStatus;

	private Integer retryCount;

	private LocalDateTime processedAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	public static MessageEvent createProcessing(MessagePublishCommand command, MessageTemplateSnapshot template) {
		return MessageEvent.builder()
				.eventNo(command.getEventNo())
				.sourceSystem(command.getSourceSystem())
				.messageType(command.getMessageType())
				.priority(MessageTypeUtils.parsePriority(command.getMessageType()))
				.occurredAt(command.getOccurredAt() == null ? LocalDateTime.now() : command.getOccurredAt())
				.targetType(command.getTargetType())
				.targetUserIds(command.getTargetUserIds())
				.templateId(template.getTemplateId())
				.tempTemplate(command.getTempTemplate())
				.params(command.getParams())
				.contentFormat(ContentFormat.TEXT)
				.actionUrl(command.getActionUrl())
				.dispatchStatus(DispatchStatus.PROCESSING)
				.retryCount(0)
				.build();
	}

	public boolean isCompleted() {
		return dispatchStatus == DispatchStatus.COMPLETED;
	}
}
