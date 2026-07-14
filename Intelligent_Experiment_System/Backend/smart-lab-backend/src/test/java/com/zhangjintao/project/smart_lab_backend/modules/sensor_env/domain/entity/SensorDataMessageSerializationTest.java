package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDataMessageSerializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Test
	void shouldSerializeJsonWithCamelCaseFields() throws Exception {
		SensorDataMessage message = SensorDataMessage.builder()
				.messageId("message-001")
				.eventNo("iot.sensor.data.report:1234567890123456")
				.occurredAt(LocalDateTime.of(2026, 7, 10, 10, 0))
				.sourceSystem(SensorEnvConstants.SOURCE_SYSTEM)
				.sensor(SensorDataMessage.SensorInfo.builder()
						.sensorId(1L)
						.sensorNo("SENSOR-001")
						.sensorTypeId(2L)
						.sensorTypeName("温湿度传感器")
						.build())
				.target(SensorDataMessage.TargetInfo.builder()
						.targetType(2)
						.targetId(100L)
						.build())
				.detectedData(Map.of("temperature", SensorDataMessage.DataItem.builder()
						.value(new BigDecimal("26.5"))
						.unit("°C")
						.build()))
				.detectedAt(LocalDateTime.of(2026, 7, 10, 9, 59, 58))
				.build();

		String json = objectMapper.writeValueAsString(message);
		JsonNode root = objectMapper.readTree(json);

		assertThat(root.get("messageId").asText()).isEqualTo("message-001");
		assertThat(root.get("eventNo").asText()).startsWith("iot.sensor.data.report:");
		assertThat(root.get("sourceSystem").asText()).isEqualTo("iot");
		assertThat(root.get("sensor").get("sensorNo").asText()).isEqualTo("SENSOR-001");
		assertThat(root.get("target").get("targetType").asInt()).isEqualTo(2);
		assertThat(root.get("detectedData").get("temperature").get("unit").asText()).isEqualTo("°C");
		assertThat(root.has("status")).isFalse();
		assertThat(json).doesNotContain("message_id", "event_no", "source_system", "detected_data");
	}
}
