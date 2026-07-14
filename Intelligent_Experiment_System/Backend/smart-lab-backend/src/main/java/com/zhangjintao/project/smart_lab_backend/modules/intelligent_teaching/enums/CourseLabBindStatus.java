package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseLabBindStatus {

	BOUND(1, "已绑定"),
	UNBOUND(2, "已解绑");

	private final int code;
	private final String description;
}
