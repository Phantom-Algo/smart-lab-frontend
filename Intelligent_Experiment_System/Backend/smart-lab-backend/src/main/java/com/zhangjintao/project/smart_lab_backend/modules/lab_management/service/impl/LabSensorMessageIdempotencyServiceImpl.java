package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorMessageIdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabSensorMessageIdempotencyServiceImpl implements LabSensorMessageIdempotencyService {

	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public boolean isProcessed(String eventNo) {
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(
				LabManagementConstants.labSensorMessageProcessedRedisKey(eventNo)));
	}

	@Override
	public void markProcessed(String eventNo) {
		stringRedisTemplate.opsForValue().set(LabManagementConstants.labSensorMessageProcessedRedisKey(eventNo),
				"1", LabManagementConstants.LAB_SENSOR_MESSAGE_PROCESSED_TTL);
	}
}
