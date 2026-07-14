package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 死信处理状态。
 */
@Getter
@RequiredArgsConstructor
public enum DeadLetterStatus {

	PENDING(0, "待处理"),
	RESOLVED(1, "已解决"),
	IGNORED(2, "已忽略");

	private final int code;
	private final String description;

	public static DeadLetterStatus fromCode(int code) {
		for (DeadLetterStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的死信处理状态：" + code);
	}
}
