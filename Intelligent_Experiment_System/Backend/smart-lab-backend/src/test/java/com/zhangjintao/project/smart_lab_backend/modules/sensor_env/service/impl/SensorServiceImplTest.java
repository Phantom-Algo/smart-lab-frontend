package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.SensorProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorStatusNotificationService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorServiceImplTest {

	@Mock private SensorRepository sensorRepository;
	@Mock private SensorTypeRepository sensorTypeRepository;
	@Mock private SensorTypeService sensorTypeService;
	@Mock private LabSensorBindingService labSensorBindingService;
	@Mock private DeviceSensorBindingService deviceSensorBindingService;
	@Mock private SensorRuntimeCacheManager cacheManager;
	@Mock private SensorStatusNotificationService notificationService;
	@Mock private SensorProperties properties;
	@InjectMocks private SensorServiceImpl sensorService;

	@Test
	void createShouldLockTypeSaveAndBindLab() {
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorType()));
		when(sensorRepository.existsBySensorNoIncludingDeleted("SENSOR-001")).thenReturn(false);
		when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> {
			Sensor sensor = invocation.getArgument(0);
			sensor.setId(10L);
			sensor.setCreatedAt(LocalDateTime.now());
			return sensor;
		});

		var response = sensorService.create(SensorCreateCommand.builder()
				.sensorNo("SENSOR-001")
				.sensorTypeId(1L)
				.targetType(2)
				.targetId(100L)
				.remark("  实验室传感器  ")
				.build());

		assertThat(response.getSensorNo()).isEqualTo("SENSOR-001");
		assertThat(response.getRemark()).isEqualTo("实验室传感器");
		verify(labSensorBindingService).bind(10L, 100L);
		verify(deviceSensorBindingService, never()).bind(any(), any());
		verify(cacheManager).evict("SENSOR-001");
	}

	@Test
	void createShouldRejectDeletedSensorNoReuse() {
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorType()));
		when(sensorRepository.existsBySensorNoIncludingDeleted("SENSOR-001")).thenReturn(true);

		assertThatThrownBy(() -> sensorService.create(SensorCreateCommand.builder()
				.sensorNo("SENSOR-001").sensorTypeId(1L).targetType(2).targetId(100L).build()))
				.isInstanceOf(SensorEnvException.class)
				.extracting("code")
				.isEqualTo(SensorEnvErrorCode.SENSOR_NO_DUPLICATE.getCode());
		verify(sensorRepository, never()).save(any());
	}

	@Test
	void updateShouldBindNewDeviceAndUnbindOldLab() {
		Sensor sensor = sensor();
		when(sensorRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sensor));
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorType()));
		when(sensorRepository.updateInfo(sensor)).thenReturn(sensor);

		sensorService.update(10L, SensorUpdateCommand.builder()
				.sensorTypeId(1L).targetType(1).targetId(200L).build());

		verify(deviceSensorBindingService).bind(10L, 200L);
		verify(labSensorBindingService).unbind(10L, 100L);
		assertThat(sensor.getTargetType()).isEqualTo(SensorTargetType.DEVICE);
	}

	@Test
	void clearFaultShouldBecomeOfflineWhenHeartbeatExpired() {
		Sensor sensor = sensor();
		sensor.setRunningStatus(SensorRunningStatus.FAULT);
		sensor.setCreatedAt(LocalDateTime.now().minusMinutes(10));
		when(sensorRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sensor));
		when(properties.getOfflineThreshold()).thenReturn(Duration.ofMinutes(5));
		when(sensorTypeService.listAll()).thenReturn(List.of(SensorTypeListItemResponse.builder()
				.id(1L).sensorTypeName("温湿度传感器").build()));

		var response = sensorService.updateStatus(10L, SensorStatusUpdateCommand.builder()
				.runningStatus(1).reason("维修完成").build());

		assertThat(response.getRunningStatus()).isEqualTo(SensorRunningStatus.OFFLINE.getCode());
		verify(sensorRepository).updateRuntime(sensor);
		verify(notificationService).publishAfterCommit(sensor, SensorRunningStatus.FAULT,
				SensorRunningStatus.OFFLINE, "维修完成");
	}

	private Sensor sensor() {
		return Sensor.builder()
				.id(10L).sensorNo("SENSOR-001").sensorTypeId(1L)
				.targetType(SensorTargetType.LAB).targetId(100L)
				.runningStatus(SensorRunningStatus.NORMAL)
				.createdAt(LocalDateTime.now()).build();
	}

	private SensorType sensorType() {
		return SensorType.builder().id(1L).sensorTypeName("温湿度传感器").build();
	}
}
