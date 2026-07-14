package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息投递目标类型。
 */
@Getter
@RequiredArgsConstructor
public enum TargetType {

	SINGLE("single", "单个用户"),
	MULTI("multi", "多个用户"),
	ALL("all", "全部用户"),
	ALL_STUDENT("all_student", "全部学生"),
	ALL_TEACHER("all_teacher", "全部教师"),
	ALL_MANAGER("all_manager", "全部管理员");

	private final String code;
	private final String description;

	public static TargetType fromCode(String code) {
		for (TargetType targetType : values()) {
			if (targetType.code.equals(code)) {
				return targetType;
			}
		}
		throw new IllegalArgumentException("未知的消息投递目标类型：" + code);
	}

	public boolean requiresExplicitTargets() {
		return this == SINGLE || this == MULTI;
	}
}
