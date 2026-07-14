package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionAlertItem;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabEnvironmentAlertNotifier;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabEnvironmentAlertRecipientResolver;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.utils.LabAlertMessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabEnvironmentAlertNotifierImpl implements LabEnvironmentAlertNotifier {

	private final LabEnvironmentAlertRecipientResolver recipientResolver;
	private final MessagePublisher messagePublisher;
	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public void notifyIfNecessary(LabSensorDataConsumeResult result) {
		if (result == null || !result.hasAlert()) {
			return;
		}
		List<DetectionAlertItem> alertItems = listUncooledAlertItems(result);
		if (alertItems.isEmpty()) {
			return;
		}
		List<Long> recipientUserIds = recipientResolver.resolveRecipients(result);
		if (recipientUserIds.isEmpty()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_ENVIRONMENT_ALERT_RECIPIENT_NOT_FOUND,
					"实验室环境预警未解析到接收人，labId=" + result.getLab().getId());
		}
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo(LabAlertMessageEventNoGenerator.generate(result.getEventNo()))
				.sourceSystem(LabManagementConstants.ENVIRONMENT_ALERT_SOURCE_SYSTEM)
				.messageType(LabManagementConstants.ENVIRONMENT_ALERT_MESSAGE_TYPE)
				.targetType(TargetType.MULTI)
				.targetUserIds(recipientUserIds)
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("实验室环境异常预警")
						.contentTemplate(buildAlertContent(result, alertItems))
						.contentFormat(ContentFormat.TEXT)
						.build())
				.occurredAt(result.getDetectedAt())
				.build());
		markCooldown(result, alertItems);
	}

	private List<DetectionAlertItem> listUncooledAlertItems(LabSensorDataConsumeResult result) {
		return result.getAlertItems().stream()
				.filter(item -> !Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey(result, item))))
				.toList();
	}

	private void markCooldown(LabSensorDataConsumeResult result, List<DetectionAlertItem> alertItems) {
		for (DetectionAlertItem alertItem : alertItems) {
			stringRedisTemplate.opsForValue().set(cooldownKey(result, alertItem), "1",
					LabManagementConstants.LAB_ENV_ALERT_COOLDOWN_TTL);
		}
	}

	private String cooldownKey(LabSensorDataConsumeResult result, DetectionAlertItem item) {
		Long sensorId = result.getSensor() == null ? null : result.getSensor().getSensorId();
		return LabManagementConstants.labEnvironmentAlertCooldownRedisKey(result.getLab().getId(), sensorId,
				item.getIndicatorName(), item.getExceedDirection().getCode());
	}

	private String buildAlertContent(LabSensorDataConsumeResult result, List<DetectionAlertItem> alertItems) {
		Lab lab = result.getLab();
		StringBuilder builder = new StringBuilder()
				.append("检测到实验室环境指标异常。")
				.append("\n实验室：").append(lab.getLabName())
				.append("\n传感器：").append(resolveSensorDisplay(result))
				.append("\n采集时间：").append(result.getDetectedAt())
				.append("\nIoT事件：").append(result.getEventNo())
				.append("\n异常指标：");
		for (DetectionAlertItem item : alertItems) {
			builder.append("\n- ")
					.append(item.getIndicatorName())
					.append(" 当前值=").append(item.getActualValue())
					.append(item.getUnit() == null ? "" : item.getUnit())
					.append("，安全范围=").append(item.getSafeRange())
					.append("，方向=").append(item.getExceedDirection().getDescription());
		}
		return builder.toString();
	}

	private String resolveSensorDisplay(LabSensorDataConsumeResult result) {
		if (result.getSensor() == null) {
			return "未知传感器";
		}
		String sensorNo = result.getSensor().getSensorNo();
		String sensorTypeName = result.getSensor().getSensorTypeName();
		if (sensorTypeName == null || sensorTypeName.isBlank()) {
			return sensorNo;
		}
		return sensorNo + "（" + sensorTypeName + "）";
	}
}
