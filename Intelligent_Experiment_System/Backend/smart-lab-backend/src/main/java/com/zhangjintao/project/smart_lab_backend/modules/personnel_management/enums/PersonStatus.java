package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonStatus {

	NORMAL(1, "正常"),
	DISABLED(2, "停用"),
	FROZEN(3, "冻结"),
	LEFT(4, "离组");

	private final int code;
	private final String description;
}
