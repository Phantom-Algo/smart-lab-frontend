package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LabCloseType {

	HOLIDAY(1, "节假日"),
	EQUIPMENT_MAINTENANCE(2, "设备维保"),
	TEMPORARY_CLOSED(3, "临时关闭"),
	OTHER(4, "其他");

	private final Integer code;
	private final String description;

	public static LabCloseType fromCode(Integer code) {
		for (LabCloseType type : values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("未知的实验室关闭类型：" + code);
	}
}
