package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {

	ONLINE(1, "在线"),
	LOGGED_OUT(2, "已退出"),
	EXPIRED(3, "已过期"),
	INVALIDATED(4, "已失效");

	private final int code;
	private final String description;

	public static SessionStatus fromCode(int code) {
		for (SessionStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的会话状态：" + code);
	}
}
