package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.LabIotSummaryResponse;

public interface LabIotSummaryService {
	LabIotSummaryResponse buildSummary(Long labId);
}
