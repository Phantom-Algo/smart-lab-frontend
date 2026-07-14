package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseStatus {

	ONGOING(1, "进行中"),
	FINISHED(2, "已结课"),
	ARCHIVED(3, "已归档");

	private final int code;
	private final String description;
}
