package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorStatusNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorRuntimeServiceImplTest {

	@Mock private SensorRepository sensorRepository;
	@Mock private SensorRuntimeCacheManager cacheManager;
	@Mock private SensorStatusNotificationService notificationService;
	@InjectMocks private SensorRuntimeServiceImpl service;

	@Test
	void offlineSensorShouldRecoverOnSuccessfulReport() {
		Sensor sensor = sensor(SensorRunningStatus.OFFLINE);
		LocalDateTime now = LocalDateTime.now();
		when(sensorRepository.findBySensorNoForUpdate("SENSOR-001")).thenReturn(Optional.of(sensor));

		var snapshot = service.recordSuccessfulReport("SENSOR-001", now);

		assertThat(snapshot.getRunningStatus()).isEqualTo(SensorRunningStatus.NORMAL.getCode());
		assertThat(sensor.getLastReportAt()).isEqualTo(now);
		verify(sensorRepository).updateRuntime(sensor);
		verify(cacheManager).evict("SENSOR-001");
		verify(notificationService).publishAfterCommit(sensor, SensorRunningStatus.OFFLINE,
				SensorRunningStatus.NORMAL, "收到合法数据上报");
	}

	@Test
	void faultSensorShouldRejectReport() {
		Sensor sensor = sensor(SensorRunningStatus.FAULT);
		when(sensorRepository.findBySensorNoForUpdate("SENSOR-001")).thenReturn(Optional.of(sensor));

		assertThatThrownBy(() -> service.recordSuccessfulReport("SENSOR-001", LocalDateTime.now()))
				.isInstanceOf(SensorEnvException.class);
		verify(sensorRepository, never()).updateRuntime(sensor);
	}

	private Sensor sensor(SensorRunningStatus status) {
		return Sensor.builder().id(1L).sensorNo("SENSOR-001").sensorTypeId(1L)
				.targetType(SensorTargetType.LAB).targetId(100L).runningStatus(status)
				.createdAt(LocalDateTime.now().minusMinutes(10)).build();
	}
}
