package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataReceiveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorWorkLogQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataReceiveResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorWorkLogResponse;

/**
 * 传感器数据服务接口。
 * <p>
 * 负责接收外部模拟程序推送的传感器检测数据并持久化，以及提供历史日志查询。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
public interface SensorDataService {

	/**
	 * 接收并持久化传感器检测数据。
	 * <p>
	 * 在一笔事务中完成传感器状态校验、数据字段名校验、时间校验、
	 * 心跳更新和日志写入。故障传感器拒绝上报，离线传感器允许上报并自动恢复。
	 * </p>
	 *
	 * @param command 数据接收命令
	 * @return 接收结果（logId、status）
	 */
	SensorDataReceiveResponse receive(SensorDataReceiveCommand command);

	/**
	 * 按传感器ID和时间范围分页查询历史工作日志。
	 *
	 * @param criteria 查询条件（含 sensorId、时间范围、分页）
	 * @return 分页工作日志列表
	 */
	PageResult<SensorWorkLogResponse> queryLogs(SensorWorkLogQueryCriteria criteria);

	/**
	 * 查询传感器最新一条工作日志。
	 *
	 * @param sensorId 传感器ID
	 * @return 最新日志，无记录返回 null
	 */
	SensorWorkLogResponse getLatestLog(Long sensorId);
}
