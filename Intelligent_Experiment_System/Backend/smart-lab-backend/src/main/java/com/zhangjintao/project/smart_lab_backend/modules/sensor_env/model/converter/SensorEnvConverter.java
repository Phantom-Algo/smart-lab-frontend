package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataReceiveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.ReceiveSensorDataRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.SensorPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorStatusRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorWorkLogResponse;

import java.util.List;

public final class SensorEnvConverter {

	private SensorEnvConverter() {
	}

	public static SensorTypeCreateCommand toCreateCommand(CreateSensorTypeRequest request) {
		return SensorTypeCreateCommand.builder()
				.sensorTypeName(request.getSensorTypeName())
				.description(request.getDescription())
				.build();
	}

	public static SensorTypeUpdateCommand toUpdateCommand(UpdateSensorTypeRequest request) {
		return SensorTypeUpdateCommand.builder()
				.sensorTypeName(request.getSensorTypeName())
				.description(request.getDescription())
				.build();
	}

	public static SensorDataTypeCreateCommand toCreateCommand(CreateSensorDataTypeRequest request) {
		return SensorDataTypeCreateCommand.builder()
				.dataName(request.getDataName())
				.unit(request.getUnit())
				.build();
	}

	public static SensorDataTypeUpdateCommand toUpdateCommand(UpdateSensorDataTypeRequest request) {
		return SensorDataTypeUpdateCommand.builder()
				.dataName(request.getDataName())
				.unit(request.getUnit())
				.build();
	}

	public static SensorCreateCommand toCreateCommand(CreateSensorRequest request) {
		return SensorCreateCommand.builder()
				.sensorNo(request.getSensorNo())
				.sensorTypeId(request.getSensorTypeId())
				.targetType(request.getTargetType())
				.targetId(request.getTargetId())
				.remark(request.getRemark())
				.build();
	}

	public static SensorUpdateCommand toUpdateCommand(UpdateSensorRequest request) {
		return SensorUpdateCommand.builder()
				.sensorTypeId(request.getSensorTypeId())
				.targetType(request.getTargetType())
				.targetId(request.getTargetId())
				.remark(request.getRemark())
				.build();
	}

	public static SensorStatusUpdateCommand toStatusUpdateCommand(UpdateSensorStatusRequest request) {
		return SensorStatusUpdateCommand.builder()
				.runningStatus(request.getRunningStatus())
				.reason(request.getReason())
				.build();
	}

	public static SensorQueryCriteria toCriteria(SensorPageQueryRequest request) {
		return SensorQueryCriteria.builder()
				.keyword(request.getKeyword())
				.sensorTypeId(request.getSensorTypeId())
				.targetType(request.getTargetType())
				.runningStatus(request.getRunningStatus())
				.page(request.getPage())
				.pageSize(request.getPageSize())
				.sortField(request.getSortField())
				.sortOrder(request.getSortOrder())
				.build();
	}

	public static SensorResponse toResponse(Sensor sensor, String sensorTypeName) {
		return SensorResponse.builder()
				.id(sensor.getId())
				.sensorNo(sensor.getSensorNo())
				.sensorTypeId(sensor.getSensorTypeId())
				.sensorTypeName(sensorTypeName)
				.targetType(sensor.getTargetType().getCode())
				.targetTypeDescription(sensor.getTargetType().getDescription())
				.targetId(sensor.getTargetId())
				.runningStatus(sensor.getRunningStatus().getCode())
				.runningStatusDescription(sensor.getRunningStatus().getDescription())
				.lastReportAt(sensor.getLastReportAt())
				.remark(sensor.getRemark())
				.createdAt(sensor.getCreatedAt())
				.updatedAt(sensor.getUpdatedAt())
				.build();
	}

	public static SensorTypeListItemResponse toListItemResponse(SensorType sensorType) {
		List<SensorDataType> dataTypes = sensorType.getDataTypes() == null ? List.of() : sensorType.getDataTypes();
		return SensorTypeListItemResponse.builder()
				.id(sensorType.getId())
				.sensorTypeName(sensorType.getSensorTypeName())
				.description(sensorType.getDescription())
				.dataTypeCount(dataTypes.size())
				.createdAt(sensorType.getCreatedAt())
				.updatedAt(sensorType.getUpdatedAt())
				.build();
	}

	public static SensorTypeDetailResponse toDetailResponse(SensorType sensorType) {
		List<SensorDataType> dataTypes = sensorType.getDataTypes() == null ? List.of() : sensorType.getDataTypes();
		return SensorTypeDetailResponse.builder()
				.id(sensorType.getId())
				.sensorTypeName(sensorType.getSensorTypeName())
				.description(sensorType.getDescription())
				.dataTypes(dataTypes.stream().map(SensorEnvConverter::toDataTypeResponse).toList())
				.createdAt(sensorType.getCreatedAt())
				.updatedAt(sensorType.getUpdatedAt())
				.build();
	}

	public static SensorDataTypeResponse toDataTypeResponse(SensorDataType sensorDataType) {
		return SensorDataTypeResponse.builder()
				.id(sensorDataType.getId())
				.sensorTypeId(sensorDataType.getSensorTypeId())
				.dataName(sensorDataType.getDataName())
				.unit(sensorDataType.getUnit())
				.createdAt(sensorDataType.getCreatedAt())
				.updatedAt(sensorDataType.getUpdatedAt())
				.build();
	}

	// ==================== 模块三：数据接收与日志 ====================

	public static SensorDataReceiveCommand toDataReceiveCommand(ReceiveSensorDataRequest request) {
		return SensorDataReceiveCommand.builder()
				.sensorNo(request.getSensorNo())
				.detectedData(request.getDetectedData())
				.detectedAt(request.getDetectedAt())
				.build();
	}

	public static SensorWorkLogResponse toWorkLogResponse(SensorWorkLog workLog) {
		return SensorWorkLogResponse.builder()
				.id(workLog.getId())
				.sensorId(workLog.getSensorId())
				.sensorNo(workLog.getSensorNo())
				.detectedData(workLog.getDetectedData())
				.status(workLog.getStatus())
				.detectedAt(workLog.getDetectedAt())
				.createdAt(workLog.getCreatedAt())
				.build();
	}
}
