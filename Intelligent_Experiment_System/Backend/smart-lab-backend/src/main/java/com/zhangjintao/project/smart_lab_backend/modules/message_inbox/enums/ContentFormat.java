package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息内容格式。
 */
@Getter
@RequiredArgsConstructor
public enum ContentFormat {

	TEXT("TEXT", "纯文本"),
	MARKDOWN("MARKDOWN", "Markdown"),
	HTML("HTML", "HTML");

	private final String code;
	private final String description;

	public static ContentFormat fromCode(String code) {
		for (ContentFormat format : values()) {
			if (format.code.equals(code)) {
				return format;
			}
		}
		throw new IllegalArgumentException("未知的消息内容格式：" + code);
	}
}
