package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;

public interface SensorDataRouteService {

	/**
	 * 在当前事务提交后转发传感器数据；若当前没有事务，则立即转发。
	 *
	 * @param command 传感器数据路由命令
	 */
	void publishAfterCommit(SensorDataRouteCommand command);
}
