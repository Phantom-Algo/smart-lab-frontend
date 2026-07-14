package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskSubmissionStatus {

	SUBMITTED(1, "已提交"),
	RETURNED(2, "已退回"),
	SCORED(3, "已评分");

	private final int code;
	private final String description;
}
