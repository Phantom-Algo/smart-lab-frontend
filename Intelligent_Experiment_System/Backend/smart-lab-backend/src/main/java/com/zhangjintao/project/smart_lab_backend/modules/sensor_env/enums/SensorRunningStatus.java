package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SensorRunningStatus {

	NORMAL(1, "正常"),
	OFFLINE(2, "离线"),
	FAULT(3, "故障");

	private final int code;
	private final String description;

	public static SensorRunningStatus fromCode(Integer code) {
		if (code != null) {
			for (SensorRunningStatus value : values()) {
				if (value.code == code) {
					return value;
				}
			}
		}
		throw new IllegalArgumentException("未知的传感器运行状态：" + code);
	}
}
