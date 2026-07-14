package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SourceType {

	MANUAL(1, "手工新增"),
	IMPORT(2, "批量导入"),
	SYNC(3, "外部同步");

	private final int code;
	private final String description;
}
