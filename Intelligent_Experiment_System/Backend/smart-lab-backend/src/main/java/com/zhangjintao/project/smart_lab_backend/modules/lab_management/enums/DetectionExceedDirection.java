package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DetectionExceedDirection {

	LOWER("LOWER", "低于下限"),
	UPPER("UPPER", "高于上限"),
	OUT_OF_RANGE("OUT_OF_RANGE", "不在安全区间内");

	private final String code;
	private final String description;
}
