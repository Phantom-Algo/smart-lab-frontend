package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class MessageTypeUtils {

	private static final Pattern EVENT_NO_PATTERN = Pattern.compile(MessageInboxConstants.EVENT_NO_REGEX);

	private MessageTypeUtils() {
	}

	public static void validateMessageType(String messageType) {
		if (!StringUtils.hasText(messageType)) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "messageType 不能为空");
		}
		String[] segments = messageType.split("\\.");
		if (segments.length < 3) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"messageType 至少需要包含 系统.优先级类型.具体事件 三段");
		}
		try {
			MessagePriority.fromMessageTypeSegment(segments[1]);
		}
		catch (IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"messageType 第二段只能是 warning/system/normal");
		}
	}

	public static void validateEventNo(String eventNo, String messageType) {
		validateMessageType(messageType);
		if (!StringUtils.hasText(eventNo)) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "eventNo 不能为空");
		}
		if (!EVENT_NO_PATTERN.matcher(eventNo).matches()) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"eventNo 必须符合 {messageType}:{16位数字} 格式");
		}
		if (!eventNo.startsWith(messageType + ":")) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT,
					"eventNo 前缀必须与 messageType 保持一致");
		}
	}

	public static MessagePriority parsePriority(String messageType) {
		validateMessageType(messageType);
		return MessagePriority.fromMessageTypeSegment(messageType.split("\\.")[1]);
	}
}
