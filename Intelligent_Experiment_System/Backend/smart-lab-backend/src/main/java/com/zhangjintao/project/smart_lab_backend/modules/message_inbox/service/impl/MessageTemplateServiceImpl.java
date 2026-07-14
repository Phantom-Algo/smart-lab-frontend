package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgTemplateRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 消息模板服务实现
 */
@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl implements MessageTemplateService {

	private final MsgTemplateRepository msgTemplateRepository;

	@Override
	public MessageTemplateSnapshot resolveTemplate(MessagePublishCommand command) {
		if (command.getTempTemplate() != null) {
			return fromTempTemplate(command.getTempTemplate());
		}
		if (command.getTemplateId() != null) {
			return msgTemplateRepository.findEnabledById(command.getTemplateId())
					.map(this::fromTemplate)
					.orElseThrow(() -> new MessageInboxException(MessageInboxErrorCode.TEMPLATE_NOT_FOUND,
							"消息模板不存在或未启用，templateId=" + command.getTemplateId()));
		}
		String templateCode = resolveTemplateCode(command);
		return msgTemplateRepository.findEnabledByCode(templateCode)
				.map(this::fromTemplate)
				.orElseThrow(() -> new MessageInboxException(MessageInboxErrorCode.TEMPLATE_NOT_FOUND,
						"消息模板不存在或未启用，templateCode=" + templateCode));
	}

	private String resolveTemplateCode(MessagePublishCommand command) {
		if (StringUtils.hasText(command.getTemplateCode())) {
			return command.getTemplateCode();
		}
		return command.getMessageType();
	}

	private MessageTemplateSnapshot fromTempTemplate(TempMessageTemplate tempTemplate) {
		if (!StringUtils.hasText(tempTemplate.getTitleTemplate()) || !StringUtils.hasText(tempTemplate.getContentTemplate())) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_NOT_FOUND, "临时模板标题和正文不能为空");
		}
		ContentFormat contentFormat = tempTemplate.getContentFormat() == null ? ContentFormat.TEXT : tempTemplate.getContentFormat();
		return MessageTemplateSnapshot.builder()
				.titleTemplate(tempTemplate.getTitleTemplate())
				.contentTemplate(tempTemplate.getContentTemplate())
				.contentFormat(contentFormat)
				.paramSchema(tempTemplate.getParamSchema())
				.build();
	}

	private MessageTemplateSnapshot fromTemplate(MessageTemplate template) {
		return MessageTemplateSnapshot.builder()
				.templateId(template.getId())
				.titleTemplate(template.getTitleTemplate())
				.contentTemplate(template.getContentTemplate())
				.contentFormat(template.getContentFormat())
				.paramSchema(template.getParamSchema())
				.build();
	}
}
