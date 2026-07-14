package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;

import java.util.List;

public interface LabEnvironmentAlertRecipientResolver {

	List<Long> resolveRecipients(LabSensorDataConsumeResult result);
}
