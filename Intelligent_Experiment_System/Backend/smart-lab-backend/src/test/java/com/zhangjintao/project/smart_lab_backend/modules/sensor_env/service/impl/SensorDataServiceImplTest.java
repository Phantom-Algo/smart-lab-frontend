package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorWorkLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataReceiveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataRouteService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeCatalogCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceImplTest {

	@Mock private SensorRuntimeService sensorRuntimeService;
	@Mock private SensorWorkLogRepository sensorWorkLogRepository;
	@Mock private SensorRepository sensorRepository;
	@Mock private SensorTypeCatalogCacheManager typeCatalogCacheManager;
	@Mock private SensorDataRouteService sensorDataRouteService;
	@InjectMocks private SensorDataServiceImpl sensorDataService;

	@Test
	void receiveShouldPersistAndRegisterRouteMessage() {
		Map<String, BigDecimal> detectedData = new LinkedHashMap<>();
		detectedData.put("temperature", new BigDecimal("26.5"));
		detectedData.put("humidity", new BigDecimal("58.2"));
		SensorRuntimeSnapshot snapshot = SensorRuntimeSnapshot.builder()
				.sensorId(1L)
				.sensorNo("SENSOR-001")
				.sensorTypeId(2L)
				.targetType(2)
				.targetId(100L)
				.runningStatus(SensorRunningStatus.NORMAL.getCode())
				.build();
		when(sensorRuntimeService.getBySensorNo("SENSOR-001")).thenReturn(snapshot);
		when(typeCatalogCacheManager.listAll(any())).thenReturn(List.of(sensorType()));
		when(sensorWorkLogRepository.save(any(SensorWorkLog.class))).thenAnswer(invocation -> {
			SensorWorkLog workLog = invocation.getArgument(0);
			return SensorWorkLog.builder()
					.id(99L)
					.sensorId(workLog.getSensorId())
					.sensorNo(workLog.getSensorNo())
					.detectedData(workLog.getDetectedData())
					.status(workLog.getStatus())
					.detectedAt(workLog.getDetectedAt())
					.build();
		});

		var response = sensorDataService.receive(SensorDataReceiveCommand.builder()
				.sensorNo(" SENSOR-001 ")
				.detectedData(detectedData)
				.detectedAt("2026-07-10T10:00:00")
				.build());

		assertThat(response.getLogId()).isEqualTo(99L);
		verify(sensorRuntimeService).recordSuccessfulReport(eq("SENSOR-001"),
				eq(LocalDateTime.of(2026, 7, 10, 10, 0)));
		ArgumentCaptor<SensorDataRouteCommand> routeCaptor =
				ArgumentCaptor.forClass(SensorDataRouteCommand.class);
		verify(sensorDataRouteService).publishAfterCommit(routeCaptor.capture());
		SensorDataRouteCommand routeCommand = routeCaptor.getValue();
		assertThat(routeCommand.getLogId()).isEqualTo(99L);
		assertThat(routeCommand.getSensorNo()).isEqualTo("SENSOR-001");
		assertThat(routeCommand.getSensorTypeName()).isEqualTo("温湿度传感器");
		assertThat(routeCommand.getTargetType()).isEqualTo(2);
		assertThat(routeCommand.getTargetId()).isEqualTo(100L);
		assertThat(routeCommand.getDetectedData().get("temperature").getUnit()).isEqualTo("°C");
		assertThat(routeCommand.getDetectedData().get("temperature").getValue())
				.isEqualByComparingTo("26.5");
		assertThat(routeCommand.getDetectedData().get("humidity").getUnit()).isEqualTo("%RH");
	}

	private SensorType sensorType() {
		return SensorType.builder()
				.id(2L)
				.sensorTypeName("温湿度传感器")
				.dataTypes(List.of(
						SensorDataType.builder()
								.sensorTypeId(2L)
								.dataName("temperature")
								.unit("°C")
								.build(),
						SensorDataType.builder()
								.sensorTypeId(2L)
								.dataName("humidity")
								.unit("%RH")
								.build()))
				.build();
	}
}
