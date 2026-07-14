package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorDataTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorWorkLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;

import java.util.List;

final class SensorEnvPersistenceConverter {

	private SensorEnvPersistenceConverter() {
	}

	static SensorType toSensorType(SensorTypeDO dataObject) {
		return SensorType.builder()
				.id(dataObject.getId())
				.sensorTypeName(dataObject.getSensorTypeName())
				.description(dataObject.getDescription())
				.dataTypes(List.of())
				.createdAt(dataObject.getCreatedAt())
				.updatedAt(dataObject.getUpdatedAt())
				.build();
	}

	static SensorTypeDO toSensorTypeDO(SensorType sensorType) {
		SensorTypeDO dataObject = new SensorTypeDO();
		dataObject.setId(sensorType.getId());
		dataObject.setSensorTypeName(sensorType.getSensorTypeName());
		dataObject.setDescription(sensorType.getDescription());
		dataObject.setCreatedAt(sensorType.getCreatedAt());
		dataObject.setUpdatedAt(sensorType.getUpdatedAt());
		return dataObject;
	}

	static SensorDataType toSensorDataType(SensorDataTypeDO dataObject) {
		return SensorDataType.builder()
				.id(dataObject.getId())
				.sensorTypeId(dataObject.getSensorTypeId())
				.dataName(dataObject.getDataName())
				.unit(dataObject.getUnit())
				.createdAt(dataObject.getCreatedAt())
				.updatedAt(dataObject.getUpdatedAt())
				.build();
	}

	static SensorDataTypeDO toSensorDataTypeDO(SensorDataType sensorDataType) {
		SensorDataTypeDO dataObject = new SensorDataTypeDO();
		dataObject.setId(sensorDataType.getId());
		dataObject.setSensorTypeId(sensorDataType.getSensorTypeId());
		dataObject.setDataName(sensorDataType.getDataName());
		dataObject.setUnit(sensorDataType.getUnit());
		dataObject.setCreatedAt(sensorDataType.getCreatedAt());
		dataObject.setUpdatedAt(sensorDataType.getUpdatedAt());
		return dataObject;
	}

	static Sensor toSensor(SensorDO dataObject) {
		return Sensor.builder()
				.id(dataObject.getId())
				.sensorNo(dataObject.getSensorNo())
				.sensorTypeId(dataObject.getSensorTypeId())
				.targetType(SensorTargetType.fromCode(dataObject.getTargetType()))
				.targetId(dataObject.getTargetId())
				.runningStatus(SensorRunningStatus.fromCode(dataObject.getRunningStatus()))
				.lastReportAt(dataObject.getLastReportAt())
				.remark(dataObject.getRemark())
				.createdAt(dataObject.getCreatedAt())
				.updatedAt(dataObject.getUpdatedAt())
				.deletedAt(dataObject.getDeletedAt())
				.build();
	}

	static SensorDO toSensorDO(Sensor sensor) {
		SensorDO dataObject = new SensorDO();
		dataObject.setId(sensor.getId());
		dataObject.setSensorNo(sensor.getSensorNo());
		dataObject.setSensorTypeId(sensor.getSensorTypeId());
		dataObject.setTargetType(sensor.getTargetType().getCode());
		dataObject.setTargetId(sensor.getTargetId());
		dataObject.setRunningStatus(sensor.getRunningStatus().getCode());
		dataObject.setLastReportAt(sensor.getLastReportAt());
		dataObject.setRemark(sensor.getRemark());
		dataObject.setCreatedAt(sensor.getCreatedAt());
		dataObject.setUpdatedAt(sensor.getUpdatedAt());
		dataObject.setDeletedAt(sensor.getDeletedAt());
		return dataObject;
	}

	// ==================== SensorWorkLog 转换 ====================

	/**
	 * DO → Entity 转换（不包含 JSON 反序列化，detectedData 字段由调用方填充）。
	 */
	static SensorWorkLog toSensorWorkLog(SensorWorkLogDO dataObject) {
		return SensorWorkLog.builder()
				.id(dataObject.getId())
				.sensorId(dataObject.getSensorId())
				.sensorNo(dataObject.getSensorNo())
				.detectedData(null) // 由 Repository 层填充
				.status(dataObject.getStatus())
				.detectedAt(dataObject.getDetectedAt())
				.createdAt(dataObject.getCreatedAt())
				.build();
	}

	/**
	 * Entity → DO 转换（不包含 JSON 序列化，detectedData 字段由调用方填充）。
	 */
	static SensorWorkLogDO toSensorWorkLogDO(SensorWorkLog workLog) {
		SensorWorkLogDO dataObject = new SensorWorkLogDO();
		dataObject.setId(workLog.getId());
		dataObject.setSensorId(workLog.getSensorId());
		dataObject.setSensorNo(workLog.getSensorNo());
		dataObject.setDetectedData(null); // 由 Repository 层填充
		dataObject.setStatus(workLog.getStatus());
		dataObject.setDetectedAt(workLog.getDetectedAt());
		dataObject.setCreatedAt(workLog.getCreatedAt());
		return dataObject;
	}
}
