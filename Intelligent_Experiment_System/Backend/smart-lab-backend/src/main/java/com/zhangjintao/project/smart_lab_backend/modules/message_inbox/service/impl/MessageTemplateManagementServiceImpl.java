package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgTemplateRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.CreateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.MessageTemplatePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.UpdateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.MessageTemplateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateManagementService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageTypeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class MessageTemplateManagementServiceImpl implements MessageTemplateManagementService {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(MessageInboxConstants.MESSAGE_PLACEHOLDER_REGEX);

	private final ObjectMapper objectMapper;
	private final MsgTemplateRepository msgTemplateRepository;
	private final CurrentUserQueryService currentUserQueryService;

	@Override
	@Transactional
	public MessageTemplateResponse create(CreateMessageTemplateRequest request) {
		validateTemplateDefinition(request.getMessageType(), request.getTitleTemplate(), request.getContentTemplate(),
				request.getParamSchema());
		if (msgTemplateRepository.existsByCode(request.getTemplateCode())) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_ALREADY_EXISTS,
					"消息模板编码已存在，templateCode=" + request.getTemplateCode());
		}
		Long currentUserId = currentUserId();
		MessageTemplate template = MessageTemplate.builder()
				.templateCode(request.getTemplateCode())
				.templateName(request.getTemplateName())
				.messageType(request.getMessageType())
				.titleTemplate(request.getTitleTemplate())
				.contentTemplate(request.getContentTemplate())
				.contentFormat(resolveContentFormat(request.getContentFormat()))
				.paramSchema(serializeParamSchema(request.getParamSchema()))
				.status(resolveStatus(request.getStatus()))
				.createdBy(currentUserId)
				.updatedBy(currentUserId)
				.build();
		try {
			return toResponse(msgTemplateRepository.insert(template));
		}
		catch (DuplicateKeyException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_ALREADY_EXISTS,
					"消息模板编码已存在，templateCode=" + request.getTemplateCode());
		}
	}

	@Override
	public MessageTemplateResponse getById(Long id) {
		return toResponse(getTemplateOrThrow(id));
	}

	@Override
	public PageResult<MessageTemplateResponse> page(MessageTemplatePageQueryRequest request) {
		TemplateStatus status = request.getStatus() == null ? null : resolveStatus(request.getStatus());
		PageResult<MessageTemplate> pageResult = msgTemplateRepository.page(request.getPageNum(), request.getPageSize(),
				request.getTemplateCode(), request.getTemplateName(), request.getMessageType(), status);
		return PageResult.of(pageResult.getRecords().stream()
				.map(this::toResponse)
				.toList(), pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
	}

	@Override
	@Transactional
	public MessageTemplateResponse update(Long id, UpdateMessageTemplateRequest request) {
		MessageTemplate existingTemplate = getTemplateOrThrow(id);
		String messageType = request.getMessageType() == null ? existingTemplate.getMessageType()
				: request.getMessageType();
		String titleTemplate = request.getTitleTemplate() == null ? existingTemplate.getTitleTemplate()
				: request.getTitleTemplate();
		String contentTemplate = request.getContentTemplate() == null ? existingTemplate.getContentTemplate()
				: request.getContentTemplate();
		JsonNode paramSchema = request.isParamSchemaSpecified() ? request.getParamSchema()
				: parseParamSchema(existingTemplate.getParamSchema());
		validateTemplateDefinition(messageType, titleTemplate, contentTemplate, paramSchema);
		MessageTemplate template = MessageTemplate.builder()
				.id(existingTemplate.getId())
				.templateCode(existingTemplate.getTemplateCode())
				.templateName(request.getTemplateName() == null ? existingTemplate.getTemplateName()
						: request.getTemplateName())
				.messageType(messageType)
				.titleTemplate(titleTemplate)
				.contentTemplate(contentTemplate)
				.contentFormat(request.getContentFormat() == null ? existingTemplate.getContentFormat()
						: request.getContentFormat())
				.paramSchema(request.isParamSchemaSpecified() ? serializeParamSchema(request.getParamSchema())
						: existingTemplate.getParamSchema())
				.status(request.getStatus() == null ? existingTemplate.getStatus() : resolveStatus(request.getStatus()))
				.createdBy(existingTemplate.getCreatedBy())
				.updatedBy(currentUserId())
				.build();
		return toResponse(msgTemplateRepository.update(template));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		getTemplateOrThrow(id);
		msgTemplateRepository.disable(id, currentUserId());
	}

	private MessageTemplate getTemplateOrThrow(Long id) {
		return msgTemplateRepository.findById(id)
				.orElseThrow(() -> new MessageInboxException(MessageInboxErrorCode.TEMPLATE_NOT_FOUND,
						"消息模板不存在，id=" + id));
	}

	private void validateTemplateDefinition(String messageType, String titleTemplate, String contentTemplate, JsonNode paramSchema) {
		MessageTypeUtils.validateMessageType(messageType);
		Set<String> placeholders = extractPlaceholders(titleTemplate + "\n" + contentTemplate);
		if (paramSchema == null || paramSchema.isNull()) {
			if (!placeholders.isEmpty()) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
						"paramSchema 为空时不允许模板包含占位符");
			}
			return;
		}
		if (!paramSchema.isObject()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "paramSchema 必须是 JSON 对象");
		}
		JsonNode propertiesNode = paramSchema.path("properties");
		if (!propertiesNode.isObject()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "paramSchema.properties 必须是对象");
		}
		JsonNode requiredNode = paramSchema.path("required");
		if (!requiredNode.isMissingNode() && !requiredNode.isArray()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "paramSchema.required 必须是数组");
		}
		for (String placeholder : placeholders) {
			if (!propertiesNode.has(placeholder)) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
						"模板占位符未在 paramSchema.properties 中声明：" + placeholder);
			}
		}
	}

	private Set<String> extractPlaceholders(String templateText) {
		Set<String> placeholders = new java.util.HashSet<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
		while (matcher.find()) {
			placeholders.add(matcher.group(1));
		}
		return placeholders;
	}

	private String serializeParamSchema(JsonNode paramSchema) {
		if (paramSchema == null || paramSchema.isNull()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(paramSchema);
		}
		catch (JsonProcessingException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "paramSchema 序列化失败");
		}
	}

	private JsonNode parseParamSchema(String paramSchema) {
		if (paramSchema == null) {
			return null;
		}
		try {
			return objectMapper.readTree(paramSchema);
		}
		catch (JsonProcessingException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "paramSchema 反序列化失败");
		}
	}

	private ContentFormat resolveContentFormat(ContentFormat contentFormat) {
		return contentFormat == null ? ContentFormat.TEXT : contentFormat;
	}

	private TemplateStatus resolveStatus(Integer status) {
		if (status == null) {
			return TemplateStatus.ENABLED;
		}
		try {
			return TemplateStatus.fromCode(status);
		}
		catch (IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板状态只能是0或1");
		}
	}

	private Long currentUserId() {
		AuthenticatedUser currentUser = currentUserQueryService.requireCurrentUser();
		return currentUser.getUserId();
	}

	private MessageTemplateResponse toResponse(MessageTemplate template) {
		return MessageTemplateResponse.from(template, objectMapper);
	}
}
