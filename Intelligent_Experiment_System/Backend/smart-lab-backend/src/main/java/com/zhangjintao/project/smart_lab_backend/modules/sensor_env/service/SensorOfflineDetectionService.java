package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.SensorProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorOfflineDetectionService {

	private final SensorRepository sensorRepository;
	private final SensorProperties properties;
	private final SensorRuntimeCacheManager cacheManager;
	private final SensorStatusNotificationService notificationService;

	public int detectOfflineSensors() {
		LocalDateTime cutoff = LocalDateTime.now().minus(properties.getOfflineThreshold());
		int total = 0;
		while (true) {
			List<Sensor> candidates = sensorRepository.listOfflineCandidates(cutoff,
					properties.getOfflineScanBatchSize());
			for (Sensor sensor : candidates) {
				if (sensorRepository.updateStatusIfCurrent(sensor.getId(), SensorRunningStatus.NORMAL,
						SensorRunningStatus.OFFLINE)) {
					total++;
					cacheManager.evict(sensor.getSensorNo());
					notificationService.publishAfterCommit(sensor, SensorRunningStatus.NORMAL,
							SensorRunningStatus.OFFLINE, "超过离线阈值未上报数据");
				}
			}
			if (candidates.size() < properties.getOfflineScanBatchSize()) {
				return total;
			}
		}
	}
}
