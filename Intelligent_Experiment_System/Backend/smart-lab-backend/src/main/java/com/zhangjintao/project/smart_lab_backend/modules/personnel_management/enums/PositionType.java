package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionType {

	STUDENT(1, "学生岗位"),
	TEACHER(2, "教师岗位"),
	MANAGEMENT(3, "管理岗位");

	private final int code;
	private final String description;
}
