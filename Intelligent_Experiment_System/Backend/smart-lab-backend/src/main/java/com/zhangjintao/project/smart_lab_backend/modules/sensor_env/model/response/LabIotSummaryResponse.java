package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * 实验室 IoT 数据汇总响应（供 Unity 3D 场景展示使用）。
 */
@Value
@Builder
public class LabIotSummaryResponse {

	Long labId;
	BigDecimal temperature;
	BigDecimal humidity;
	BigDecimal smoke;
	BigDecimal gas;
	BigDecimal battery;
	BigDecimal signalStrength;
	String status;
	String lastUpdateStr;
	List<SensorSummaryItem> sensors;

	@Value
	@Builder
	public static class SensorSummaryItem {
		Long sensorId;
		String sensorNo;
		String sensorTypeName;
		String dataName;
		BigDecimal value;
		String unit;
	}
}
