package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 传感器数据接收命令 —— Service 层入参。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataReceiveCommand {

	/** 传感器编号 */
	private String sensorNo;

	/** 检测数据，key为数据名称，value为数值 */
	private Map<String, BigDecimal> detectedData;

	/** 传感器采集时间（ISO 8601），null 表示使用服务端时间 */
	private String detectedAt;
}
