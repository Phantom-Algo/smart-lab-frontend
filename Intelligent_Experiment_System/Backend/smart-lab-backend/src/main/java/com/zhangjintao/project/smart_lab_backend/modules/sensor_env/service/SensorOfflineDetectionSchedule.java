package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-lab.iot.sensor", name = "status-detection-enabled", havingValue = "true")
public class SensorOfflineDetectionSchedule {

	private final SensorOfflineDetectionService detectionService;

	@Scheduled(fixedDelayString = "${smart-lab.iot.sensor.offline-scan-fixed-delay:1m}")
	public void detectOfflineSensors() {
		int changed = detectionService.detectOfflineSensors();
		if (changed > 0) {
			log.info("传感器离线扫描完成，状态变更数量={}", changed);
		}
	}
}
