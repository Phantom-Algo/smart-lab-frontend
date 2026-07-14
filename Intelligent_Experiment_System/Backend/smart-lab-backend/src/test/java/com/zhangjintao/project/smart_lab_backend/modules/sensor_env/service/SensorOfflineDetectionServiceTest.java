package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.SensorProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorOfflineDetectionServiceTest {

	@Mock private SensorRepository sensorRepository;
	@Mock private SensorProperties properties;
	@Mock private SensorRuntimeCacheManager cacheManager;
	@Mock private SensorStatusNotificationService notificationService;
	@InjectMocks private SensorOfflineDetectionService service;

	@Test
	void shouldNotifyOnlyWhenAtomicTransitionSucceeds() {
		Sensor sensor = Sensor.builder().id(1L).sensorNo("SENSOR-001").sensorTypeId(1L)
				.targetType(SensorTargetType.LAB).targetId(100L)
				.runningStatus(SensorRunningStatus.NORMAL).build();
		when(properties.getOfflineThreshold()).thenReturn(Duration.ofMinutes(5));
		when(properties.getOfflineScanBatchSize()).thenReturn(200);
		when(sensorRepository.listOfflineCandidates(any(), any(Integer.class))).thenReturn(List.of(sensor));
		when(sensorRepository.updateStatusIfCurrent(1L, SensorRunningStatus.NORMAL,
				SensorRunningStatus.OFFLINE)).thenReturn(true);

		assertThat(service.detectOfflineSensors()).isEqualTo(1);
		verify(cacheManager).evict("SENSOR-001");
		verify(notificationService).publishAfterCommit(sensor, SensorRunningStatus.NORMAL,
				SensorRunningStatus.OFFLINE, "超过离线阈值未上报数据");
	}
}
