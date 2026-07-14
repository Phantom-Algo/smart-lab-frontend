package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionStatus {

	ACTIVE(1, "有效"),
	INACTIVE(2, "失效");

	private final int code;
	private final String description;

	public static PermissionStatus fromCode(int code) {
		for (PermissionStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的权限状态：" + code);
	}
}
