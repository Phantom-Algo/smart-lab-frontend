package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SensorTargetType {

	DEVICE(1, "设备"),
	LAB(2, "实验室");

	private final int code;
	private final String description;

	public static SensorTargetType fromCode(Integer code) {
		if (code != null) {
			for (SensorTargetType value : values()) {
				if (value.code == code) {
					return value;
				}
			}
		}
		throw new IllegalArgumentException("未知的传感器目标类型：" + code);
	}
}
