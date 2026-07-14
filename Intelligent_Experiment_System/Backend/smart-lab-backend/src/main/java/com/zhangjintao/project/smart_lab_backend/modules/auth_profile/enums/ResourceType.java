package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {

	EQUIPMENT(1, "器材"),
	SITE(2, "场地");

	private final int code;
	private final String description;

	public static ResourceType fromCode(int code) {
		for (ResourceType type : values()) {
			if (type.code == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("未知的资源类型：" + code);
	}
}
