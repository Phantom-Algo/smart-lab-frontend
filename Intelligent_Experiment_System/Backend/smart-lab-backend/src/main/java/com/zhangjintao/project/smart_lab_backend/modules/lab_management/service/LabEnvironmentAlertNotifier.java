package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;

public interface LabEnvironmentAlertNotifier {

	void notifyIfNecessary(LabSensorDataConsumeResult result);
}
