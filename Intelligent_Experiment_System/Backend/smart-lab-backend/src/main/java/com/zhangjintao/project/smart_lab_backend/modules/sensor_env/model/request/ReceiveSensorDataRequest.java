package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 接收模拟传感器数据请求。
 * <p>
 * 供外部模拟程序通过 POST /api/iot/sensor-data 推送传感器检测数据。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
public class ReceiveSensorDataRequest {

	/** 传感器编号，必须已注册且未删除 */
	@NotBlank(message = "传感器编号不能为空")
	private String sensorNo;

	/** 检测数据，key 匹配 sensor_data_type.data_name，至少包含一个数据项 */
	@NotEmpty(message = "检测数据不能为空")
	private Map<String, BigDecimal> detectedData;

	/** 检测时间（ISO 8601），不传则使用服务端时间 */
	private String detectedAt;
}
