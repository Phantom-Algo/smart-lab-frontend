package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

public interface LabSensorBindingService {
	void bind(Long sensorId, Long labId);
	void unbind(Long sensorId, Long labId);
	boolean existsActiveBindingByLabId(Long labId);
}
