package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublishValidator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageTypeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 消息发布校验实现
 */
@Component
public class MessagePublishValidatorImpl implements MessagePublishValidator {

	@Override
	public void validate(MessagePublishCommand command) {
		if (command == null) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "消息发布命令不能为空");
		}
		MessageTypeUtils.validateEventNo(command.getEventNo(), command.getMessageType());
		if (!StringUtils.hasText(command.getSourceSystem())) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "sourceSystem 不能为空");
		}
		if (command.getTargetType() == null) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "targetType 不能为空");
		}
		validateTargets(command);
		validateTemplateSource(command);
	}

	private void validateTargets(MessagePublishCommand command) {
		TargetType targetType = command.getTargetType();
		if (targetType.requiresExplicitTargets()
				&& (command.getTargetUserIds() == null || command.getTargetUserIds().isEmpty())) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"single/multi 类型消息必须指定 targetUserIds");
		}
	}

	private void validateTemplateSource(MessagePublishCommand command) {
		boolean hasTemplateId = command.getTemplateId() != null;
		boolean hasTemplateCode = StringUtils.hasText(command.getTemplateCode());
		boolean hasTempTemplate = command.getTempTemplate() != null;
		if (hasTempTemplate && (hasTemplateId || hasTemplateCode)) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"tempTemplate 不能与 templateId/templateCode 同时指定");
		}
		if (hasTemplateId && hasTemplateCode) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"templateId 与 templateCode 不能同时指定");
		}
	}
}
