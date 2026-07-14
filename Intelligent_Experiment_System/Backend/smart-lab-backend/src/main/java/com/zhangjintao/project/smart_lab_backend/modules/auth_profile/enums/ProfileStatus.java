package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProfileStatus {

	NORMAL(1, "正常"),
	DISABLED(2, "停用");

	private final int code;
	private final String description;

	public static ProfileStatus fromCode(int code) {
		for (ProfileStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的资料状态：" + code);
	}
}
