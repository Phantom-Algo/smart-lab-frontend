package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionAlertItem;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionExceedDirection;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabEnvironmentAlertRecipientResolver;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabEnvironmentAlertNotifierImplTest {

	@Mock
	private LabEnvironmentAlertRecipientResolver recipientResolver;

	@Mock
	private MessagePublisher messagePublisher;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private LabEnvironmentAlertNotifierImpl notifier;

	@Test
	void notifyIfNecessaryShouldPublishLabWarningMessageAndMarkCooldown() {
		LabSensorDataConsumeResult result = alertResult();
		when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
		when(recipientResolver.resolveRecipients(result)).thenReturn(List.of(1L, 2L));
		when(messagePublisher.publish(any(MessagePublishCommand.class))).thenReturn(MessagePublishResult.builder()
				.eventNo("lab.warning.environment_alert:1234567890123456")
				.streamRecordId("1-0")
				.build());
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

		notifier.notifyIfNecessary(result);

		ArgumentCaptor<MessagePublishCommand> commandCaptor = ArgumentCaptor.forClass(MessagePublishCommand.class);
		verify(messagePublisher).publish(commandCaptor.capture());
		MessagePublishCommand command = commandCaptor.getValue();
		assertThat(command.getEventNo()).isEqualTo("lab.warning.environment_alert:1234567890123456");
		assertThat(command.getSourceSystem()).isEqualTo("lab");
		assertThat(command.getMessageType()).isEqualTo("lab.warning.environment_alert");
		assertThat(command.getTargetType()).isEqualTo(TargetType.MULTI);
		assertThat(command.getTargetUserIds()).containsExactly(1L, 2L);
		assertThat(command.getTempTemplate().getContentTemplate()).contains("化学实验室", "temperature");
		verify(valueOperations).set(eq("lab:env:alert:cooldown:100:200:temperature:UPPER"), eq("1"),
				eq(LabManagementConstants.LAB_ENV_ALERT_COOLDOWN_TTL));
	}

	private LabSensorDataConsumeResult alertResult() {
		return LabSensorDataConsumeResult.builder()
				.eventNo("iot.sensor.data.report:1234567890123456")
				.lab(Lab.builder()
						.id(100L)
						.labName("化学实验室")
						.build())
				.sensor(SensorDataMessage.SensorInfo.builder()
						.sensorId(200L)
						.sensorNo("SENSOR-001")
						.sensorTypeName("温湿度传感器")
						.build())
				.detectedAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.status(DetectionStatus.ALERT)
				.alertItems(List.of(DetectionAlertItem.builder()
						.indicatorName("temperature")
						.actualValue(BigDecimal.valueOf(35))
						.unit("℃")
						.safeRange(List.of(List.of(BigDecimal.valueOf(18), BigDecimal.valueOf(30))))
						.exceedDirection(DetectionExceedDirection.UPPER)
						.build()))
				.build();
	}
}
