package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeUpdateCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorType {

	private Long id;
	private String sensorTypeName;
	private String description;
	@Builder.Default
	private List<SensorDataType> dataTypes = List.of();
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static SensorType create(SensorTypeCreateCommand command) {
		return SensorType.builder()
				.sensorTypeName(command.getSensorTypeName())
				.description(command.getDescription())
				.build();
	}

	public void applyUpdate(SensorTypeUpdateCommand command) {
		this.sensorTypeName = command.getSensorTypeName();
		this.description = command.getDescription();
	}
}
