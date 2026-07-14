package com.zhangjintao.project.smart_lab_backend.modules.lab_management.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabEnvironmentAlertNotifier;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorDataConsumeService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorMessageIdempotencyService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
		topic = "${smart-lab.iot.mq.topic:iot-sensor-data}",
		selectorType = SelectorType.TAG,
		selectorExpression = "${smart-lab.iot.mq.tags.lab-sensor-data:lab_sensor_data}",
		consumerGroup = "${smart-lab.iot.mq.consumer-groups.lab-sensor-data-consumer:lab-sensor-data-consumer}"
)
public class LabSensorDataConsumer implements RocketMQListener<MessageExt> {

	private final ObjectMapper objectMapper;
	private final LabSensorDataConsumeService labSensorDataConsumeService;
	private final LabEnvironmentAlertNotifier labEnvironmentAlertNotifier;
	private final LabSensorMessageIdempotencyService idempotencyService;

	@Override
	public void onMessage(MessageExt messageExt) {
		String rocketMqMsgId = messageExt.getMsgId();
		String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
		try {
			SensorDataMessage sensorMessage = objectMapper.readValue(body, SensorDataMessage.class);
			if (!isLabSensorMessage(sensorMessage)) {
				log.debug("忽略非实验室传感器消息，rocketMqMsgId={}, targetType={}", rocketMqMsgId,
						sensorMessage.getTarget() == null ? null : sensorMessage.getTarget().getTargetType());
				return;
			}
			if (StringUtils.hasText(sensorMessage.getEventNo()) && idempotencyService.isProcessed(sensorMessage.getEventNo())) {
				log.debug("实验室传感器消息已处理，eventNo={}, rocketMqMsgId={}", sensorMessage.getEventNo(), rocketMqMsgId);
				return;
			}
			LabSensorDataConsumeResult result = labSensorDataConsumeService.recordAndEvaluate(sensorMessage);
			labEnvironmentAlertNotifier.notifyIfNecessary(result);
			idempotencyService.markProcessed(result.getEventNo());
			log.info("实验室传感器消息处理完成，eventNo={}, labId={}, status={}, duplicate={}",
					result.getEventNo(), result.getLab().getId(), result.getStatus(), result.isDuplicate());
		}
		catch (Exception ex) {
			log.error("实验室传感器MQ消息处理失败，rocketMqMsgId={}, body={}", rocketMqMsgId, body, ex);
			if (ex instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException(ex);
		}
	}

	private boolean isLabSensorMessage(SensorDataMessage sensorMessage) {
		if (sensorMessage == null || sensorMessage.getTarget() == null) {
			return false;
		}
		return Integer.valueOf(SensorTargetType.LAB.getCode()).equals(sensorMessage.getTarget().getTargetType());
	}
}
