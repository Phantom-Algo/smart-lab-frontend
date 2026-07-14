package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {

	STUDENT(1, "学生", "ROLE_STUDENT"),
	TEACHER(2, "教师", "ROLE_TEACHER"),
	ADMIN(3, "管理员", "ROLE_ADMIN");

	private final int code;
	private final String description;
	private final String authority;

	public static UserType fromCode(int code) {
		for (UserType userType : values()) {
			if (userType.code == code) {
				return userType;
			}
		}
		throw new IllegalArgumentException("未知的用户类型：" + code);
	}
}
