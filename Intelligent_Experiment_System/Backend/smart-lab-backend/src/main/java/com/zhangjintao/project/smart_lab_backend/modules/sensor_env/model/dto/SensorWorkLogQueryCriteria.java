package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 传感器工作日志查询条件。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorWorkLogQueryCriteria {

	/** 传感器ID */
	private Long sensorId;

	/** 查询起始时间 */
	private LocalDateTime startTime;

	/** 查询结束时间 */
	private LocalDateTime endTime;

	/** 页码，最小1 */
	private Long page;

	/** 每页条数，1～1000 */
	private Long pageSize;
}
