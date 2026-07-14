package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseGradePublishStatus {

	UNPUBLISHED(1, "未发布"),
	PUBLISHED(2, "已发布"),
	LOCKED(3, "已锁定");

	private final int code;
	private final String description;
}
