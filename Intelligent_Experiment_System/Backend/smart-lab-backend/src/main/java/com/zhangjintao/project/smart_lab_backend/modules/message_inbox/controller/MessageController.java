package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.PublishMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.PublishMessageResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessagePublisher messagePublisher;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<PublishMessageResponse> publish(@Valid @RequestBody PublishMessageRequest request) {
		MessagePublishResult result = messagePublisher.publish(toCommand(request));
		return ApiResult.success(PublishMessageResponse.from(result));
	}

	private MessagePublishCommand toCommand(PublishMessageRequest request) {
		String eventNo = MessageEventNoGenerator.generate(request.getMessageType());
		return MessagePublishCommand.builder()
				.eventNo(eventNo)
				.sourceSystem(resolveSourceSystem(request.getMessageType()))
				.messageType(request.getMessageType())
				.targetType(resolveTargetType(request.getTargetType()))
				.targetUserIds(request.getTargetUserIds())
				.templateCode(request.getTemplateCode())
				.tempTemplate(resolveTempTemplate(request))
				.params(request.getParams())
				.actionUrl(request.getActionUrl())
				.build();
	}

	private String resolveSourceSystem(String messageType) {
		return messageType.substring(0, messageType.indexOf('.'));
	}

	private TargetType resolveTargetType(String targetType) {
		try {
			return TargetType.fromCode(targetType);
		}
		catch (IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"未知的消息投递目标类型：" + targetType);
		}
	}

	private TempMessageTemplate resolveTempTemplate(PublishMessageRequest request) {
		if (request.hasTemplateCode()) {
			return null;
		}
		return TempMessageTemplate.builder()
				.titleTemplate(request.getTitle())
				.contentTemplate(request.getContent())
				.contentFormat(resolveContentFormat(request))
				.build();
	}

	private ContentFormat resolveContentFormat(PublishMessageRequest request) {
		return request.getContentFormat() == null ? ContentFormat.TEXT : request.getContentFormat();
	}
}
