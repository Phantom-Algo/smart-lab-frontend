package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgEventRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.value.MessageContentSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageDispatchResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.RenderedMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxDeliveryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageDispatchProcessor;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublishValidator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.RecipientResolveService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.TemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息分发处理服务实现
 */
@Service
@RequiredArgsConstructor
public class MessageDispatchProcessorImpl implements MessageDispatchProcessor {

	private final MsgEventRepository msgEventRepository;
	private final MessagePublishValidator messagePublishValidator;
	private final MessageTemplateService messageTemplateService;
	private final TemplateRenderService templateRenderService;
	private final RecipientResolveService recipientResolveService;
	private final InboxDeliveryService inboxDeliveryService;

	@Override
	public MessageDispatchResult process(MessagePublishCommand command) {
		messagePublishValidator.validate(command);
		MessageEvent event = msgEventRepository.findByEventNo(command.getEventNo()).orElse(null);
		if (event != null && event.isCompleted()) {
			return MessageDispatchResult.builder()
					.duplicateCompleted(true)
					.eventId(event.getId())
					.build();
		}
		MessageTemplateSnapshot template = messageTemplateService.resolveTemplate(command);
		event = ensureEvent(command, template, event);
		if (event.isCompleted()) {
			return MessageDispatchResult.builder()
					.duplicateCompleted(true)
					.eventId(event.getId())
					.build();
		}
		msgEventRepository.markProcessing(event.getId());
		RenderedMessage renderedMessage = templateRenderService.render(template, command.getParams());
		List<Long> recipientUserIds = recipientResolveService.resolveRecipients(command);
		inboxDeliveryService.deliver(event.getId(), recipientUserIds);
		msgEventRepository.markCompleted(event.getId(), MessageContentSnapshot.of(renderedMessage.getTitle(),
				renderedMessage.getContent(), renderedMessage.getContentFormat()));
		return MessageDispatchResult.builder()
				.duplicateCompleted(false)
				.eventId(event.getId())
				.build();
	}

	@Override
	public void markFailed(String eventNo) {
		msgEventRepository.findByEventNo(eventNo)
				.ifPresent(event -> msgEventRepository.markFailed(event.getId()));
	}

	private MessageEvent ensureEvent(MessagePublishCommand command, MessageTemplateSnapshot template, MessageEvent existingEvent) {
		if (existingEvent != null) {
			return existingEvent;
		}
		return insertEvent(command, template);
	}

	private MessageEvent insertEvent(MessagePublishCommand command, MessageTemplateSnapshot template) {
		MessageEvent event = MessageEvent.createProcessing(command, template);
		try {
			return msgEventRepository.insert(event);
		}
		catch (DuplicateKeyException ex) {
			return msgEventRepository.findByEventNo(command.getEventNo()).orElseThrow(() -> ex);
		}
	}
}
