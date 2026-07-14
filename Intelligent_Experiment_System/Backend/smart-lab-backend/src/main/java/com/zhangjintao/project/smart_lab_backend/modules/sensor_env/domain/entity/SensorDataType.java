package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataType {

	private Long id;
	private Long sensorTypeId;
	private String dataName;
	private String unit;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static SensorDataType create(Long sensorTypeId, SensorDataTypeCreateCommand command) {
		return SensorDataType.builder()
				.sensorTypeId(sensorTypeId)
				.dataName(command.getDataName())
				.unit(command.getUnit())
				.build();
	}

	public void applyUpdate(SensorDataTypeUpdateCommand command) {
		this.dataName = command.getDataName();
		this.unit = command.getUnit();
	}
}
