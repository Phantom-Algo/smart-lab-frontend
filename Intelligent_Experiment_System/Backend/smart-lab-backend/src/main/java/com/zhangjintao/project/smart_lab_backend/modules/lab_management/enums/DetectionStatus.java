package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DetectionStatus {

	NORMAL(1, "正常"),
	ALERT(2, "异常告警");

	private final Integer code;
	private final String description;

	public static DetectionStatus fromCode(Integer code) {
		for (DetectionStatus status : values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的实验室检测日志状态：" + code);
	}
}
