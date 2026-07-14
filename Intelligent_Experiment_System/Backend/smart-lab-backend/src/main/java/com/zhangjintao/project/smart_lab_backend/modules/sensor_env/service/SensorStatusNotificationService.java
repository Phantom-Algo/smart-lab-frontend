package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SensorStatusNotificationService {

	private final MessagePublisher messagePublisher;

	public void publishAfterCommit(Sensor sensor, SensorRunningStatus fromStatus,
			SensorRunningStatus toStatus, String reason) {
		Runnable publisher = () -> publish(sensor, fromStatus, toStatus, reason);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publisher.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publisher.run();
			}
		});
	}

	private void publish(Sensor sensor, SensorRunningStatus fromStatus,
			SensorRunningStatus toStatus, String reason) {
		String content = "传感器运行状态已变更。"
				+ "\n传感器编号：" + sensor.getSensorNo()
				+ "\n原状态：" + fromStatus.getDescription()
				+ "\n新状态：" + toStatus.getDescription()
				+ "\n目标：" + sensor.getTargetType().getDescription() + "#" + sensor.getTargetId()
				+ "\n原因：" + reason;
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo(MessageEventNoGenerator.generate(SensorEnvConstants.SENSOR_STATUS_CHANGED_MESSAGE_TYPE))
				.sourceSystem(SensorEnvConstants.SOURCE_SYSTEM)
				.messageType(SensorEnvConstants.SENSOR_STATUS_CHANGED_MESSAGE_TYPE)
				.targetType(TargetType.ALL_MANAGER)
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("传感器运行状态变更")
						.contentTemplate(content)
						.contentFormat(ContentFormat.TEXT)
						.build())
				.occurredAt(LocalDateTime.now())
				.build());
	}
}
