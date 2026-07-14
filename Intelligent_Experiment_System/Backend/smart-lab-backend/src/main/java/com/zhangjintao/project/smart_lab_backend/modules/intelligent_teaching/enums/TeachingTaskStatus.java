package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeachingTaskStatus {

	DRAFT(1, "草稿"),
	PUBLISHED(2, "已发布"),
	CLOSED(3, "已关闭");

	private final int code;
	private final String description;
}
