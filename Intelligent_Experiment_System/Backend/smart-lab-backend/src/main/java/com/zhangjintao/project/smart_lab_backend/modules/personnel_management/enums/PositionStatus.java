package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionStatus {

	ENABLED(1, "启用"),
	DISABLED(2, "停用");

	private final int code;
	private final String description;
}
