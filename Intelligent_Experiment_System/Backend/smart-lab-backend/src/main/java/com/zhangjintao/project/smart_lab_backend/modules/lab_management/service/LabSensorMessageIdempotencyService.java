package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

public interface LabSensorMessageIdempotencyService {

	boolean isProcessed(String eventNo);

	void markProcessed(String eventNo);
}
