package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息模板状态。
 */
@Getter
@RequiredArgsConstructor
public enum TemplateStatus {

	DISABLED(0, "停用"),
	ENABLED(1, "启用");

	private final int code;
	private final String description;

	public static TemplateStatus fromCode(int code) {
		for (TemplateStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的消息模板状态：" + code);
	}
}
