package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.IotMqProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataMessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.utils.SensorDataEventNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 基于 RocketMQ 顺序消息的传感器数据生产端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMqSensorDataMessagePublisher implements SensorDataMessagePublisher {

	private final RocketMQTemplate rocketMQTemplate;
	private final ObjectMapper objectMapper;
	private final IotMqProperties iotMqProperties;

	@Override
	public void publish(SensorDataRouteCommand command) {
		String tag = resolveTag(command.getTargetType());
		String destination = iotMqProperties.getTopic() + ":" + tag;
		String eventNo = SensorDataEventNoGenerator.generate();
		SensorDataMessage message = buildMessage(command, eventNo);
		String payload = writePayload(message);

		try {
			SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, payload, command.getSensorNo());
			log.info("传感器数据顺序消息发送成功，destination={}, shardingKey={}, sendStatus={}, msgId={}, "
							+ "eventNo={}, messageId={}, logId={}",
					destination, command.getSensorNo(), sendResult.getSendStatus(), sendResult.getMsgId(),
					eventNo, message.getMessageId(), command.getLogId());
		}
		catch (Exception ex) {
			log.error("传感器数据顺序消息发送失败，destination={}, shardingKey={}, eventNo={}, messageId={}, "
							+ "logId={}, targetType={}, targetId={}",
					destination, command.getSensorNo(), eventNo, message.getMessageId(), command.getLogId(),
					command.getTargetType(), command.getTargetId(), ex);
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_ROUTE_FAILED,
					"传感器数据路由转发失败，sensorNo=" + command.getSensorNo()
					+ "，targetType=" + command.getTargetType()
					+ "，targetId=" + command.getTargetId());
		}
	}

	private SensorDataMessage buildMessage(SensorDataRouteCommand command, String eventNo) {
		return SensorDataMessage.builder()
				.messageId(UUID.randomUUID().toString())
				.eventNo(eventNo)
				.occurredAt(command.getDetectedAt())
				.sourceSystem(SensorEnvConstants.SOURCE_SYSTEM)
				.sensor(SensorDataMessage.SensorInfo.builder()
						.sensorId(command.getSensorId())
						.sensorNo(command.getSensorNo())
						.sensorTypeId(command.getSensorTypeId())
						.sensorTypeName(command.getSensorTypeName())
						.build())
				.target(SensorDataMessage.TargetInfo.builder()
						.targetType(command.getTargetType())
						.targetId(command.getTargetId())
						.build())
				.detectedData(command.getDetectedData())
				.detectedAt(command.getDetectedAt())
				.build();
	}

	private String writePayload(SensorDataMessage message) {
		try {
			return objectMapper.writeValueAsString(message);
		}
		catch (JsonProcessingException ex) {
			log.error("传感器数据消息序列化失败，messageId={}, eventNo={}",
					message.getMessageId(), message.getEventNo(), ex);
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_ROUTE_FAILED,
					"传感器数据消息序列化失败，messageId=" + message.getMessageId());
		}
	}

	private String resolveTag(Integer targetType) {
		SensorTargetType type;
		try {
			type = SensorTargetType.fromCode(targetType);
		}
		catch (IllegalArgumentException ex) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_ROUTE_FAILED,
					"未知传感器目标类型，targetType=" + targetType);
		}
		return switch (type) {
			case DEVICE -> iotMqProperties.getTags().getDeviceSensorData();
			case LAB -> iotMqProperties.getTags().getLabSensorData();
		};
	}
}
