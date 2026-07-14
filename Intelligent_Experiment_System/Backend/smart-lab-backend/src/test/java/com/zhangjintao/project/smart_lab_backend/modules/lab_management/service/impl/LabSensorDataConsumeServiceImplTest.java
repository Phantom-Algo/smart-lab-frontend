package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.DetectionIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabDetectionLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionExceedDirection;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabSensorDataConsumeServiceImplTest {

	private static final String EVENT_NO = "iot.sensor.data.report:1234567890123456";

	@Mock
	private LabRepository labRepository;

	@Mock
	private DetectionIndicatorRepository detectionIndicatorRepository;

	@Mock
	private DetectionIndicatorCacheManager detectionIndicatorCacheManager;

	@Mock
	private LabDetectionLogRepository labDetectionLogRepository;

	private ObjectMapper objectMapper;
	private LabSensorDataConsumeServiceImpl service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		service = new LabSensorDataConsumeServiceImpl(labRepository, detectionIndicatorRepository,
				detectionIndicatorCacheManager, labDetectionLogRepository, objectMapper);
	}

	@Test
	void recordAndEvaluateShouldPersistAlertLogWhenMetricExceedsSafeRange() {
		when(labRepository.findById(100L)).thenReturn(Optional.of(lab()));
		when(labDetectionLogRepository.findByEventNo(EVENT_NO)).thenReturn(Optional.empty());
		when(detectionIndicatorCacheManager.listByLabTypeId(eq(10L), any())).thenReturn(List.of(indicator()));
		when(labDetectionLogRepository.save(any(LabDetectionLog.class))).thenAnswer(invocation -> {
			LabDetectionLog detectionLog = invocation.getArgument(0);
			detectionLog.setId(99L);
			return detectionLog;
		});

		LabSensorDataConsumeResult result = service.recordAndEvaluate(message(BigDecimal.valueOf(35)));

		assertThat(result.getStatus()).isEqualTo(DetectionStatus.ALERT);
		assertThat(result.getAlertItems()).hasSize(1);
		assertThat(result.getAlertItems().getFirst().getExceedDirection()).isEqualTo(DetectionExceedDirection.UPPER);
		assertThat(result.getUnknownIndicatorNames()).containsExactly("noise");
		ArgumentCaptor<LabDetectionLog> logCaptor = ArgumentCaptor.forClass(LabDetectionLog.class);
		verify(labDetectionLogRepository).save(logCaptor.capture());
		LabDetectionLog savedLog = logCaptor.getValue();
		assertThat(savedLog.getEventNo()).isEqualTo(EVENT_NO);
		assertThat(savedLog.getMessageId()).isEqualTo("message-001");
		assertThat(savedLog.getLabId()).isEqualTo(100L);
		assertThat(savedLog.getSensorId()).isEqualTo(200L);
		assertThat(savedLog.getStatus()).isEqualTo(DetectionStatus.ALERT);
		assertThat(savedLog.getDetectedData()).contains("\"eventNo\":\"" + EVENT_NO + "\"");
		assertThat(savedLog.getAlertDetail()).contains("\"indicatorName\":\"temperature\"");
	}

	@Test
	void recordAndEvaluateShouldReturnDuplicateResultAndSkipSaveWhenEventAlreadyLogged() throws Exception {
		when(labRepository.findById(100L)).thenReturn(Optional.of(lab()));
		LabDetectionLog existingLog = LabDetectionLog.builder()
				.id(99L)
				.eventNo(EVENT_NO)
				.messageId("message-001")
				.labId(100L)
				.sensorId(200L)
				.sensorNo("SENSOR-001")
				.detectedAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.status(DetectionStatus.ALERT)
				.alertDetail(objectMapper.writeValueAsString(List.of(Map.of(
						"indicatorName", "temperature",
						"actualValue", 35,
						"unit", "℃",
						"safeRange", List.of(List.of(18, 30)),
						"exceedDirection", "UPPER"))))
				.createdAt(LocalDateTime.of(2026, 7, 10, 10, 1))
				.build();
		when(labDetectionLogRepository.findByEventNo(EVENT_NO)).thenReturn(Optional.of(existingLog));

		LabSensorDataConsumeResult result = service.recordAndEvaluate(message(BigDecimal.valueOf(35)));

		assertThat(result.isDuplicate()).isTrue();
		assertThat(result.hasAlert()).isTrue();
		assertThat(result.getDetectionLog()).isSameAs(existingLog);
		verify(labDetectionLogRepository, never()).save(any());
		verify(detectionIndicatorCacheManager, never()).listByLabTypeId(any(), any());
	}

	@Test
	void recordAndEvaluateInvalidTargetShouldThrowBusinessException() {
		SensorDataMessage message = message(BigDecimal.valueOf(20));
		message.getTarget().setTargetType(1);

		assertThatThrownBy(() -> service.recordAndEvaluate(message))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID.getCode()));

		verify(labRepository, never()).findById(any());
	}

	private Lab lab() {
		return Lab.builder()
				.id(100L)
				.labTypeId(10L)
				.labName("化学实验室")
				.runningStatus(LabRunningStatus.IN_USE)
				.dutyUserId(20L)
				.build();
	}

	private DetectionIndicator indicator() {
		return DetectionIndicator.builder()
				.id(1L)
				.labTypeId(10L)
				.indicatorName("temperature")
				.unit("℃")
				.safeRange(DetectionSafeRange.builder()
						.segments(List.of(DetectionSafeRangeSegment.builder()
								.min(BigDecimal.valueOf(18))
								.max(BigDecimal.valueOf(30))
								.build()))
						.build())
				.build();
	}

	private SensorDataMessage message(BigDecimal temperature) {
		Map<String, SensorDataMessage.DataItem> detectedData = new LinkedHashMap<>();
		detectedData.put("temperature", SensorDataMessage.DataItem.builder()
				.value(temperature)
				.unit("℃")
				.build());
		detectedData.put("noise", SensorDataMessage.DataItem.builder()
				.value(BigDecimal.valueOf(55))
				.unit("dB")
				.build());
		return SensorDataMessage.builder()
				.messageId("message-001")
				.eventNo(EVENT_NO)
				.occurredAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.sourceSystem("iot")
				.sensor(SensorDataMessage.SensorInfo.builder()
						.sensorId(200L)
						.sensorNo("SENSOR-001")
						.sensorTypeId(300L)
						.sensorTypeName("温湿度传感器")
						.build())
				.target(SensorDataMessage.TargetInfo.builder()
						.targetType(2)
						.targetId(100L)
						.build())
				.detectedData(detectedData)
				.detectedAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.build();
	}
}
