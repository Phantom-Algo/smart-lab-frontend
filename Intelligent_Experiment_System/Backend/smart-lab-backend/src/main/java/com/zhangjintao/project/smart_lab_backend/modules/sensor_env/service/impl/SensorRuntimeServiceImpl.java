package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorStatusNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SensorRuntimeServiceImpl implements SensorRuntimeService {

	private final SensorRepository sensorRepository;
	private final SensorRuntimeCacheManager cacheManager;
	private final SensorStatusNotificationService notificationService;

	@Override
	public SensorRuntimeSnapshot getBySensorNo(String sensorNo) {
		return cacheManager.get(sensorNo, () -> sensorRepository.findBySensorNo(sensorNo).map(this::toSnapshot))
				.orElseThrow(() -> sensorNotFound(sensorNo));
	}

	@Override
	@Transactional
	public SensorRuntimeSnapshot recordSuccessfulReport(String sensorNo, LocalDateTime reportedAt) {
		if (reportedAt == null) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_ENV_PARAM_INVALID, "上报时间不能为空");
		}
		Sensor sensor = sensorRepository.findBySensorNoForUpdate(sensorNo)
				.orElseThrow(() -> sensorNotFound(sensorNo));
		if (sensor.getRunningStatus() == SensorRunningStatus.FAULT) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_STATUS_TRANSITION_INVALID,
					"故障传感器不允许上报，sensorNo=" + sensorNo);
		}

		SensorRunningStatus fromStatus = sensor.getRunningStatus();
		LocalDateTime originalReportAt = sensor.getLastReportAt();
		sensor.recordReport(reportedAt);
		boolean heartbeatChanged = originalReportAt == null || sensor.getLastReportAt().isAfter(originalReportAt);
		boolean statusChanged = fromStatus != sensor.getRunningStatus();
		if (heartbeatChanged || statusChanged) {
			sensorRepository.updateRuntime(sensor);
		}
		if (statusChanged) {
			evictAfterCommit(sensorNo);
			notificationService.publishAfterCommit(sensor, fromStatus, sensor.getRunningStatus(), "收到合法数据上报");
		}
		return toSnapshot(sensor);
	}

	private SensorRuntimeSnapshot toSnapshot(Sensor sensor) {
		return SensorRuntimeSnapshot.builder()
				.sensorId(sensor.getId())
				.sensorNo(sensor.getSensorNo())
				.sensorTypeId(sensor.getSensorTypeId())
				.targetType(sensor.getTargetType().getCode())
				.targetId(sensor.getTargetId())
				.runningStatus(sensor.getRunningStatus().getCode())
				.build();
	}

	private SensorEnvException sensorNotFound(String sensorNo) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_NOT_FOUND,
				"传感器不存在或已删除，sensorNo=" + sensorNo);
	}

	private void evictAfterCommit(String sensorNo) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			cacheManager.evict(sensorNo);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cacheManager.evict(sensorNo);
			}
		});
	}
}
