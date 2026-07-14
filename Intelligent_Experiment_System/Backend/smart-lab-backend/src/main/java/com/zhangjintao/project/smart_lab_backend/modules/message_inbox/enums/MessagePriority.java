package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息优先级。
 */
@Getter
@RequiredArgsConstructor
public enum MessagePriority {

	WARNING(1, "warning", "预警通知"),
	SYSTEM(2, "system", "系统通知"),
	NORMAL(3, "normal", "普通通知");

	private final int code;
	private final String messageTypeSegment;
	private final String description;

	public static MessagePriority fromCode(int code) {
		for (MessagePriority priority : values()) {
			if (priority.code == code) {
				return priority;
			}
		}
		throw new IllegalArgumentException("未知的消息优先级：" + code);
	}

	public static MessagePriority fromMessageTypeSegment(String messageTypeSegment) {
		for (MessagePriority priority : values()) {
			if (priority.messageTypeSegment.equals(messageTypeSegment)) {
				return priority;
			}
		}
		throw new IllegalArgumentException("未知的消息类型优先级片段：" + messageTypeSegment);
	}
}
