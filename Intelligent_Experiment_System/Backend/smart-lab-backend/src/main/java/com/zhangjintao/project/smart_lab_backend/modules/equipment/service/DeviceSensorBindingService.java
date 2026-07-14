package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

public interface DeviceSensorBindingService {
	void bind(Long sensorId, Long deviceId);
	void unbind(Long sensorId, Long deviceId);
	boolean existsActiveBindingByDeviceId(Long deviceId);
}
