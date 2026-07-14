package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeachingTaskSubmitType {

	TEXT(1, "文本"),
	FILE(2, "文件"),
	MIXED(3, "混合");

	private final int code;
	private final String description;
}
