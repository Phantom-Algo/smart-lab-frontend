package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;

public interface SensorDataMessagePublisher {

	/**
	 * 将传感器数据发布到 MQ。
	 *
	 * @param command 传感器数据路由命令
	 */
	void publish(SensorDataRouteCommand command);
}
