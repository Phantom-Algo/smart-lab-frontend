package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatus {

	NORMAL(1, "正常"),
	DISABLED(2, "停用"),
	LOCKED(3, "锁定");

	private final int code;
	private final String description;

	public static AccountStatus fromCode(int code) {
		for (AccountStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的账号状态：" + code);
	}
}
