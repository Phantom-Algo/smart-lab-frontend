package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradeItemType {

	TASK(1, "任务类"),
	DAILY(2, "平时类"),
	FINAL(3, "期末类"),
	OTHER(4, "其他");

	private final int code;
	private final String description;
}
