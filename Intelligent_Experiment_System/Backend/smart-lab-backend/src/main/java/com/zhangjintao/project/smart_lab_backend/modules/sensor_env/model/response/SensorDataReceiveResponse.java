package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 传感器数据接收响应。
 * <p>
 * 不包含 alarmTriggered 字段——告警处理是下游子系统的职责，不在物联网子系统范围。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataReceiveResponse {

	/** 写入的工作日志主键ID */
	private Long logId;

	/** 数据状态：1=正常 */
	private Integer status;
}
