package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageRecipientUserPort;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.RecipientResolveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收件人解析服务实现
 */
@Service
@RequiredArgsConstructor
public class RecipientResolveServiceImpl implements RecipientResolveService {

	private final ObjectProvider<MessageRecipientUserPort> messageRecipientUserPortProvider;

	@Override
	public List<Long> resolveRecipients(MessagePublishCommand command) {
		List<Long> recipients = switch (command.getTargetType()) {
			case SINGLE, MULTI -> command.getTargetUserIds().stream().distinct().toList();
			case ALL -> userPort().listAllUserIds();
			case ALL_STUDENT -> userPort().listStudentUserIds();
			case ALL_TEACHER -> userPort().listTeacherUserIds();
			case ALL_MANAGER -> userPort().listManagerUserIds();
		};
		if (recipients == null || recipients.isEmpty()) {
			throw new MessageInboxException(MessageInboxErrorCode.RECIPIENT_RESOLVE_FAILED,
					"未解析到任何消息接收人，targetType=" + command.getTargetType().getCode());
		}
		return recipients;
	}

	private MessageRecipientUserPort userPort() {
		MessageRecipientUserPort userPort = messageRecipientUserPortProvider.getIfAvailable();
		if (userPort == null) {
			throw new MessageInboxException(MessageInboxErrorCode.RECIPIENT_RESOLVE_FAILED,
					"人员/用户模块尚未提供 MessageRecipientUserPort，无法解析群体收件人");
		}
		return userPort;
	}
}
