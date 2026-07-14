package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 传感器历史数据查询请求。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
public class SensorWorkLogQueryRequest {

	/** 查询起始时间（ISO 8601），不传默认24小时前 */
	private String startTime;

	/** 查询结束时间（ISO 8601），不传默认当前时间 */
	private String endTime;

	/** 页码，最小1 */
	@Min(value = 1, message = "页码最小为1")
	private Long page;

	/** 每页条数，1～1000 */
	@Min(value = 1, message = "每页条数最小为1")
	@Max(value = 1000, message = "每页条数最大为1000")
	private Long pageSize;
}
