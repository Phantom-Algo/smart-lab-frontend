package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgDeadLetterDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgEventDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.UserInboxMessageDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageDeadLetter;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DeadLetterStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DispatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;

import java.util.List;
import java.util.Map;

/**
 * 消息收件箱模块 DO 与领域对象转换器。
 */
public final class MessageInboxConverter {

	private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
	};

	private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP_TYPE = new TypeReference<>() {
	};

	private MessageInboxConverter() {
	}

	public static MessageEvent toMessageEvent(MsgEventDO eventDO, ObjectMapper objectMapper) {
		if (eventDO == null) {
			return null;
		}
		return MessageEvent.builder()
				.id(eventDO.getId())
				.eventNo(eventDO.getEventNo())
				.sourceSystem(eventDO.getSourceSystem())
				.messageType(eventDO.getMessageType())
				.priority(MessagePriority.fromCode(eventDO.getPriority()))
				.occurredAt(eventDO.getOccurredAt())
				.targetType(TargetType.fromCode(eventDO.getTargetType()))
				.targetUserIds(readValue(eventDO.getTargets(), LONG_LIST_TYPE, objectMapper))
				.templateId(eventDO.getTemplateId())
				.tempTemplate(readValue(eventDO.getTempTemplate(), TempMessageTemplate.class, objectMapper))
				.params(readValue(eventDO.getParams(), STRING_OBJECT_MAP_TYPE, objectMapper))
				.title(eventDO.getTitle())
				.content(eventDO.getContent())
				.contentFormat(ContentFormat.fromCode(eventDO.getContentFormat()))
				.actionUrl(eventDO.getActionUrl())
				.dispatchStatus(DispatchStatus.fromCode(eventDO.getDispatchStatus()))
				.retryCount(eventDO.getRetryCount())
				.processedAt(eventDO.getProcessedAt())
				.createdAt(eventDO.getCreatedAt())
				.updatedAt(eventDO.getUpdatedAt())
				.build();
	}

	public static MsgEventDO toMsgEventDO(MessageEvent event, ObjectMapper objectMapper) {
		if (event == null) {
			return null;
		}
		MsgEventDO eventDO = new MsgEventDO();
		eventDO.setId(event.getId());
		eventDO.setEventNo(event.getEventNo());
		eventDO.setSourceSystem(event.getSourceSystem());
		eventDO.setMessageType(event.getMessageType());
		eventDO.setPriority(event.getPriority().getCode());
		eventDO.setOccurredAt(event.getOccurredAt());
		eventDO.setTargetType(event.getTargetType().getCode());
		eventDO.setTargets(writeValue(event.getTargetUserIds(), objectMapper));
		eventDO.setTemplateId(event.getTemplateId());
		eventDO.setTempTemplate(writeValue(event.getTempTemplate(), objectMapper));
		eventDO.setParams(writeValue(event.getParams(), objectMapper));
		eventDO.setTitle(event.getTitle());
		eventDO.setContent(event.getContent());
		eventDO.setContentFormat(event.getContentFormat().getCode());
		eventDO.setActionUrl(event.getActionUrl());
		eventDO.setDispatchStatus(event.getDispatchStatus().getCode());
		eventDO.setRetryCount(event.getRetryCount());
		eventDO.setProcessedAt(event.getProcessedAt());
		eventDO.setCreatedAt(event.getCreatedAt());
		eventDO.setUpdatedAt(event.getUpdatedAt());
		return eventDO;
	}

	public static MessageTemplate toMessageTemplate(MsgTemplateDO templateDO) {
		if (templateDO == null) {
			return null;
		}
		return MessageTemplate.builder()
				.id(templateDO.getId())
				.templateCode(templateDO.getTemplateCode())
				.templateName(templateDO.getTemplateName())
				.messageType(templateDO.getMessageType())
				.titleTemplate(templateDO.getTitleTemplate())
				.contentTemplate(templateDO.getContentTemplate())
				.contentFormat(ContentFormat.fromCode(templateDO.getContentFormat()))
				.paramSchema(templateDO.getParamSchema())
				.status(TemplateStatus.fromCode(templateDO.getStatus()))
				.createdBy(templateDO.getCreatedBy())
				.updatedBy(templateDO.getUpdatedBy())
				.createdAt(templateDO.getCreatedAt())
				.updatedAt(templateDO.getUpdatedAt())
				.build();
	}

	public static MsgTemplateDO toMsgTemplateDO(MessageTemplate template) {
		if (template == null) {
			return null;
		}
		MsgTemplateDO templateDO = new MsgTemplateDO();
		templateDO.setId(template.getId());
		templateDO.setTemplateCode(template.getTemplateCode());
		templateDO.setTemplateName(template.getTemplateName());
		templateDO.setMessageType(template.getMessageType());
		templateDO.setTitleTemplate(template.getTitleTemplate());
		templateDO.setContentTemplate(template.getContentTemplate());
		if (template.getContentFormat() != null) {
			templateDO.setContentFormat(template.getContentFormat().getCode());
		}
		templateDO.setParamSchema(template.getParamSchema());
		if (template.getStatus() != null) {
			templateDO.setStatus(template.getStatus().getCode());
		}
		templateDO.setCreatedBy(template.getCreatedBy());
		templateDO.setUpdatedBy(template.getUpdatedBy());
		templateDO.setCreatedAt(template.getCreatedAt());
		templateDO.setUpdatedAt(template.getUpdatedAt());
		return templateDO;
	}

	public static UserInboxMessageDO toUserInboxMessageDO(UserInboxMessage inboxMessage) {
		if (inboxMessage == null) {
			return null;
		}
		UserInboxMessageDO inboxMessageDO = new UserInboxMessageDO();
		inboxMessageDO.setId(inboxMessage.getId());
		inboxMessageDO.setUserId(inboxMessage.getUserId());
		inboxMessageDO.setMessageId(inboxMessage.getMessageId());
		inboxMessageDO.setIsRead(inboxMessage.isRead() ? 1 : 0);
		inboxMessageDO.setReadAt(inboxMessage.getReadAt());
		inboxMessageDO.setIsDeleted(inboxMessage.isDeleted() ? 1 : 0);
		inboxMessageDO.setDeletedAt(inboxMessage.getDeletedAt());
		inboxMessageDO.setReceivedAt(inboxMessage.getReceivedAt());
		return inboxMessageDO;
	}

	public static MessageDeadLetter toMessageDeadLetter(MsgDeadLetterDO deadLetterDO) {
		if (deadLetterDO == null) {
			return null;
		}
		return MessageDeadLetter.builder()
				.id(deadLetterDO.getId())
				.eventId(deadLetterDO.getEventId())
				.eventNo(deadLetterDO.getEventNo())
				.failureStage(FailureStage.valueOf(deadLetterDO.getFailureStage()))
				.errorCode(deadLetterDO.getErrorCode())
				.errorMessage(deadLetterDO.getErrorMessage())
				.rawPayload(deadLetterDO.getRawPayload())
				.retryable(deadLetterDO.getRetryable() != null && deadLetterDO.getRetryable() == 1)
				.status(DeadLetterStatus.fromCode(deadLetterDO.getStatus()))
				.createdAt(deadLetterDO.getCreatedAt())
				.updatedAt(deadLetterDO.getUpdatedAt())
				.build();
	}

	public static MsgDeadLetterDO toMsgDeadLetterDO(MessageDeadLetter deadLetter) {
		if (deadLetter == null) {
			return null;
		}
		MsgDeadLetterDO deadLetterDO = new MsgDeadLetterDO();
		deadLetterDO.setId(deadLetter.getId());
		deadLetterDO.setEventId(deadLetter.getEventId());
		deadLetterDO.setEventNo(deadLetter.getEventNo());
		deadLetterDO.setFailureStage(deadLetter.getFailureStage().name());
		deadLetterDO.setErrorCode(deadLetter.getErrorCode());
		deadLetterDO.setErrorMessage(deadLetter.getErrorMessage());
		deadLetterDO.setRawPayload(deadLetter.getRawPayload());
		deadLetterDO.setRetryable(deadLetter.isRetryable() ? 1 : 0);
		deadLetterDO.setStatus(deadLetter.getStatus().getCode());
		deadLetterDO.setCreatedAt(deadLetter.getCreatedAt());
		deadLetterDO.setUpdatedAt(deadLetter.getUpdatedAt());
		return deadLetterDO;
	}

	private static <T> T readValue(String json, TypeReference<T> typeReference, ObjectMapper objectMapper) {
		if (json == null) {
			return null;
		}
		try {
			return objectMapper.readValue(json, typeReference);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("消息收件箱 JSON 字段反序列化失败", ex);
		}
	}

	private static <T> T readValue(String json, Class<T> valueType, ObjectMapper objectMapper) {
		if (json == null) {
			return null;
		}
		try {
			return objectMapper.readValue(json, valueType);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("消息收件箱 JSON 字段反序列化失败", ex);
		}
	}

	private static String writeValue(Object value, ObjectMapper objectMapper) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("消息收件箱 JSON 字段序列化失败", ex);
		}
	}
}
