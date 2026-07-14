package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 消息模板响应。
 */
@Value
@Builder
public class MessageTemplateResponse {

	Long id;
	String templateCode;
	String templateName;
	String messageType;
	String titleTemplate;
	String contentTemplate;
	ContentFormat contentFormat;
	JsonNode paramSchema;
	Integer status;
	Long createdBy;
	Long updatedBy;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;

	public static MessageTemplateResponse from(MessageTemplate template, ObjectMapper objectMapper) {
		return MessageTemplateResponse.builder()
				.id(template.getId())
				.templateCode(template.getTemplateCode())
				.templateName(template.getTemplateName())
				.messageType(template.getMessageType())
				.titleTemplate(template.getTitleTemplate())
				.contentTemplate(template.getContentTemplate())
				.contentFormat(template.getContentFormat())
				.paramSchema(readParamSchema(template.getParamSchema(), objectMapper))
				.status(template.getStatus() == null ? null : template.getStatus().getCode())
				.createdBy(template.getCreatedBy())
				.updatedBy(template.getUpdatedBy())
				.createdAt(template.getCreatedAt())
				.updatedAt(template.getUpdatedAt())
				.build();
	}

	private static JsonNode readParamSchema(String paramSchema, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(paramSchema)) {
			return null;
		}
		try {
			return objectMapper.readTree(paramSchema);
		}
		catch (Exception ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数定义不是合法 JSON");
		}
	}
}
