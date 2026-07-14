package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonType {

	STUDENT(1, "学生"),
	TEACHER(2, "教师"),
	ADMIN(3, "管理员");

	private final int code;
	private final String description;

	public static PersonType fromCode(int code) {
		for (PersonType value : values()) {
			if (value.code == code) {
				return value;
			}
		}
		throw new IllegalArgumentException("未知的人员类型：" + code);
	}
}
