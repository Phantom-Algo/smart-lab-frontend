package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LabOpenStatus {

	OPEN(1, "正常开放"),
	TEMPORARY_CLOSED(2, "临时关闭"),
	PERMANENT_CLOSED(3, "永久关闭");

	private final Integer code;
	private final String description;

	public static LabOpenStatus fromCode(Integer code) {
		for (LabOpenStatus status : values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的实验室开放状态：" + code);
	}
}
