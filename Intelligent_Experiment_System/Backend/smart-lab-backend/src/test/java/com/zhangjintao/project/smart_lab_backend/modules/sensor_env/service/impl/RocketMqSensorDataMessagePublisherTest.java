package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.IotMqProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RocketMqSensorDataMessagePublisherTest {

	@Mock
	private RocketMQTemplate rocketMQTemplate;

	private ObjectMapper objectMapper;
	private IotMqProperties properties;
	private RocketMqSensorDataMessagePublisher publisher;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		properties = new IotMqProperties();
		publisher = new RocketMqSensorDataMessagePublisher(rocketMQTemplate, objectMapper, properties);
	}

	@Test
	void shouldSendLabSensorDataAsOrderlyMessage() throws Exception {
		when(rocketMQTemplate.syncSendOrderly(eq("iot-sensor-data:lab_sensor_data"),
				any(Object.class), eq("SENSOR-001"))).thenReturn(sendResult());

		publisher.publish(command(2));

		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(rocketMQTemplate).syncSendOrderly(eq("iot-sensor-data:lab_sensor_data"),
				payloadCaptor.capture(), eq("SENSOR-001"));
		JsonNode root = objectMapper.readTree((String) payloadCaptor.getValue());
		assertThat(root.get("eventNo").asText()).matches("iot\\.sensor\\.data\\.report:\\d{16}");
		assertThat(root.get("sensor").get("sensorTypeName").asText()).isEqualTo("温湿度传感器");
		assertThat(root.get("target").get("targetId").asLong()).isEqualTo(100L);
		assertThat(root.get("detectedData").get("temperature").get("value").decimalValue())
				.isEqualByComparingTo("26.5");
		assertThat(root.has("status")).isFalse();
	}

	@Test
	void shouldSendDeviceSensorDataToDeviceTag() {
		when(rocketMQTemplate.syncSendOrderly(eq("iot-sensor-data:device_sensor_data"),
				any(Object.class), eq("SENSOR-001"))).thenReturn(sendResult());

		publisher.publish(command(1));

		verify(rocketMQTemplate).syncSendOrderly(eq("iot-sensor-data:device_sensor_data"),
				any(Object.class), eq("SENSOR-001"));
	}

	private SendResult sendResult() {
		SendResult sendResult = new SendResult();
		sendResult.setSendStatus(SendStatus.SEND_OK);
		sendResult.setMsgId("msg-001");
		return sendResult;
	}

	private SensorDataRouteCommand command(Integer targetType) {
		return SensorDataRouteCommand.builder()
				.logId(99L)
				.sensorId(1L)
				.sensorNo("SENSOR-001")
				.sensorTypeId(2L)
				.sensorTypeName("温湿度传感器")
				.targetType(targetType)
				.targetId(100L)
				.detectedData(Map.of("temperature", SensorDataMessage.DataItem.builder()
						.value(new BigDecimal("26.5"))
						.unit("°C")
						.build()))
				.detectedAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.build();
	}
}
