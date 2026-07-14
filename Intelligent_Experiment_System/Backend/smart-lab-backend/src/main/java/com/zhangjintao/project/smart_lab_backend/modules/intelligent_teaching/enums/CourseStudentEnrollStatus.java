package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseStudentEnrollStatus {

	ENROLLED(1, "在课"),
	WITHDRAWN(2, "退课");

	private final int code;
	private final String description;
}
