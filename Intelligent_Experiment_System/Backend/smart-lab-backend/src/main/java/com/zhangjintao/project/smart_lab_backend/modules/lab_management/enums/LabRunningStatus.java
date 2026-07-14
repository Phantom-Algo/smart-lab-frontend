package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LabRunningStatus {

	IDLE(1, "空闲"),
	IN_USE(2, "使用中"),
	MAINTENANCE(3, "维护中"),
	DISABLED(4, "停用");

	private final Integer code;
	private final String description;

	public static LabRunningStatus fromCode(Integer code) {
		for (LabRunningStatus status : values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的实验室使用状态：" + code);
	}
}
