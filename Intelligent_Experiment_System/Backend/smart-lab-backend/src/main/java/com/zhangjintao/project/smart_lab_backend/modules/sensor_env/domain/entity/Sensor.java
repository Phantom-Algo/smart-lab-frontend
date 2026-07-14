package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
	private Long id;
	private String sensorNo;
	private Long sensorTypeId;
	private SensorTargetType targetType;
	private Long targetId;
	private SensorRunningStatus runningStatus;
	private LocalDateTime lastReportAt;
	private String remark;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	public static Sensor create(SensorCreateCommand command) {
		return Sensor.builder()
				.sensorNo(command.getSensorNo())
				.sensorTypeId(command.getSensorTypeId())
				.targetType(SensorTargetType.fromCode(command.getTargetType()))
				.targetId(command.getTargetId())
				.runningStatus(SensorRunningStatus.NORMAL)
				.remark(command.getRemark())
				.build();
	}

	public void applyUpdate(SensorUpdateCommand command) {
		this.sensorTypeId = command.getSensorTypeId();
		this.targetType = SensorTargetType.fromCode(command.getTargetType());
		this.targetId = command.getTargetId();
		this.remark = command.getRemark();
	}

	public void changeStatus(SensorRunningStatus status) {
		this.runningStatus = status;
	}

	public void recordReport(LocalDateTime reportedAt) {
		if (lastReportAt == null || reportedAt.isAfter(lastReportAt)) {
			lastReportAt = reportedAt;
		}
		if (runningStatus == SensorRunningStatus.OFFLINE) {
			runningStatus = SensorRunningStatus.NORMAL;
		}
	}

	public void softDelete(LocalDateTime now) {
		this.deletedAt = now;
	}
}
