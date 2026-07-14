package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 传感器工作日志响应。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorWorkLogResponse {

	/** 日志主键ID */
	private Long id;

	/** 传感器ID */
	private Long sensorId;

	/** 传感器编号 */
	private String sensorNo;

	/** 检测数据快照，key为数据名称，value为数值 */
	private Map<String, BigDecimal> detectedData;

	/** 数据状态：1=正常 */
	private Integer status;

	/** 传感器采集时间 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime detectedAt;

	/** 写入数据库时间 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;
}
