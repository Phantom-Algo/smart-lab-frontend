package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config.SensorProperties;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorStatusNotificationService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

	private final SensorRepository sensorRepository;
	private final SensorTypeRepository sensorTypeRepository;
	private final SensorTypeService sensorTypeService;
	private final LabSensorBindingService labSensorBindingService;
	private final DeviceSensorBindingService deviceSensorBindingService;
	private final SensorRuntimeCacheManager cacheManager;
	private final SensorStatusNotificationService notificationService;
	private final SensorProperties properties;

	@Override
	@Transactional
	public SensorResponse create(SensorCreateCommand command) {
		SensorCreateCommand normalized = normalize(command);
		SensorType sensorType = requireSensorTypeForUpdate(normalized.getSensorTypeId());
		if (sensorRepository.existsBySensorNoIncludingDeleted(normalized.getSensorNo())) {
			throw duplicateSensorNo(normalized.getSensorNo());
		}
		Sensor saved = sensorRepository.save(Sensor.create(normalized));
		bindTarget(saved.getId(), saved.getTargetType(), saved.getTargetId());
		evictAfterCommit(saved.getSensorNo());
		return SensorEnvConverter.toResponse(saved, sensorType.getSensorTypeName());
	}

	@Override
	public SensorResponse getById(Long sensorId) {
		Sensor sensor = requireSensor(sensorId);
		return SensorEnvConverter.toResponse(sensor, requireSensorTypeName(sensor.getSensorTypeId()));
	}

	@Override
	public PageResult<SensorResponse> page(SensorQueryCriteria criteria) {
		PageResult<Sensor> page = sensorRepository.page(criteria);
		Map<Long, String> typeNames = sensorTypeService.listAll().stream()
				.collect(Collectors.toMap(item -> item.getId(), item -> item.getSensorTypeName()));
		return PageResult.of(page.getRecords().stream()
				.map(sensor -> SensorEnvConverter.toResponse(sensor,
						requireSensorTypeName(sensor.getSensorTypeId(), typeNames)))
				.toList(), page.getTotal(), page.getPageNum(), page.getPageSize());
	}

	@Override
	@Transactional
	public SensorResponse update(Long sensorId, SensorUpdateCommand command) {
		Sensor sensor = requireSensorForUpdate(sensorId);
		SensorUpdateCommand normalized = normalize(command);
		SensorType sensorType = requireSensorTypeForUpdate(normalized.getSensorTypeId());
		SensorTargetType newTargetType = parseTargetType(normalized.getTargetType());
		boolean targetChanged = sensor.getTargetType() != newTargetType
				|| !sensor.getTargetId().equals(normalized.getTargetId());
		if (targetChanged) {
			bindTarget(sensor.getId(), newTargetType, normalized.getTargetId());
			unbindTarget(sensor.getId(), sensor.getTargetType(), sensor.getTargetId());
		}
		sensor.applyUpdate(normalized);
		Sensor updated = sensorRepository.updateInfo(sensor);
		evictAfterCommit(updated.getSensorNo());
		return SensorEnvConverter.toResponse(updated, sensorType.getSensorTypeName());
	}

	@Override
	@Transactional
	public SensorResponse updateStatus(Long sensorId, SensorStatusUpdateCommand command) {
		Sensor sensor = requireSensorForUpdate(sensorId);
		SensorRunningStatus requested = parseManualStatus(command.getRunningStatus());
		SensorRunningStatus fromStatus = sensor.getRunningStatus();
		String reason = normalizeReason(command.getReason(), requested);

		if (requested == SensorRunningStatus.FAULT) {
			if (fromStatus == SensorRunningStatus.FAULT) {
				return SensorEnvConverter.toResponse(sensor, requireSensorTypeName(sensor.getSensorTypeId()));
			}
			sensor.changeStatus(SensorRunningStatus.FAULT);
		}
		else {
			if (fromStatus != SensorRunningStatus.FAULT) {
				throw new SensorEnvException(SensorEnvErrorCode.SENSOR_STATUS_TRANSITION_INVALID,
						"只有故障传感器可以执行解除故障，sensorId=" + sensorId);
			}
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime heartbeatBase = sensor.getLastReportAt() == null
					? sensor.getCreatedAt() : sensor.getLastReportAt();
			SensorRunningStatus effective = heartbeatBase.isBefore(now.minus(properties.getOfflineThreshold()))
					? SensorRunningStatus.OFFLINE : SensorRunningStatus.NORMAL;
			sensor.changeStatus(effective);
		}

		sensorRepository.updateRuntime(sensor);
		evictAfterCommit(sensor.getSensorNo());
		notificationService.publishAfterCommit(sensor, fromStatus, sensor.getRunningStatus(), reason);
		return SensorEnvConverter.toResponse(sensor, requireSensorTypeName(sensor.getSensorTypeId()));
	}

	@Override
	@Transactional
	public void delete(Long sensorId) {
		Sensor sensor = requireSensorForUpdate(sensorId);
		unbindTarget(sensor.getId(), sensor.getTargetType(), sensor.getTargetId());
		sensor.softDelete(LocalDateTime.now());
		sensorRepository.softDelete(sensor);
		evictAfterCommit(sensor.getSensorNo());
	}

	private SensorCreateCommand normalize(SensorCreateCommand command) {
		String sensorNo = normalizeSensorNo(command.getSensorNo());
		parseTargetType(command.getTargetType());
		requirePositive(command.getSensorTypeId(), "传感器类型ID");
		requirePositive(command.getTargetId(), "目标ID");
		return SensorCreateCommand.builder()
				.sensorNo(sensorNo)
				.sensorTypeId(command.getSensorTypeId())
				.targetType(command.getTargetType())
				.targetId(command.getTargetId())
				.remark(normalizeRemark(command.getRemark()))
				.build();
	}

	private SensorUpdateCommand normalize(SensorUpdateCommand command) {
		parseTargetType(command.getTargetType());
		requirePositive(command.getSensorTypeId(), "传感器类型ID");
		requirePositive(command.getTargetId(), "目标ID");
		return SensorUpdateCommand.builder()
				.sensorTypeId(command.getSensorTypeId())
				.targetType(command.getTargetType())
				.targetId(command.getTargetId())
				.remark(normalizeRemark(command.getRemark()))
				.build();
	}

	private String normalizeSensorNo(String sensorNo) {
		if (!StringUtils.hasText(sensorNo)) {
			throw invalidParam("传感器编号不能为空");
		}
		String normalized = sensorNo.trim();
		if (!SensorEnvConstants.SENSOR_NO_PATTERN.matcher(normalized).matches()) {
			throw invalidParam("传感器编号只能包含大写字母、数字、短横线和下划线");
		}
		return normalized;
	}

	private String normalizeRemark(String remark) {
		if (!StringUtils.hasText(remark)) {
			return null;
		}
		String normalized = remark.trim();
		if (normalized.length() > 255) {
			throw invalidParam("备注长度不能超过255");
		}
		return normalized;
	}

	private String normalizeReason(String reason, SensorRunningStatus requested) {
		if (!StringUtils.hasText(reason)) {
			if (requested == SensorRunningStatus.FAULT) {
				throw invalidParam("标记故障时必须填写原因");
			}
			return "管理员解除故障";
		}
		String normalized = reason.trim();
		if (normalized.length() > 255) {
			throw invalidParam("状态变更原因长度不能超过255");
		}
		return normalized;
	}

	private SensorTargetType parseTargetType(Integer code) {
		try {
			return SensorTargetType.fromCode(code);
		}
		catch (IllegalArgumentException ex) {
			throw invalidParam("目标类型只能是1或2");
		}
	}

	private SensorRunningStatus parseManualStatus(Integer code) {
		if (Integer.valueOf(SensorRunningStatus.NORMAL.getCode()).equals(code)) {
			return SensorRunningStatus.NORMAL;
		}
		if (Integer.valueOf(SensorRunningStatus.FAULT.getCode()).equals(code)) {
			return SensorRunningStatus.FAULT;
		}
		throw invalidParam("管理员只能提交1（解除故障）或3（标记故障）");
	}

	private void requirePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw invalidParam(fieldName + "必须为正数");
		}
	}

	private Sensor requireSensor(Long sensorId) {
		return sensorRepository.findById(sensorId).orElseThrow(() -> sensorNotFound(sensorId));
	}

	private Sensor requireSensorForUpdate(Long sensorId) {
		return sensorRepository.findByIdForUpdate(sensorId).orElseThrow(() -> sensorNotFound(sensorId));
	}

	private SensorType requireSensorTypeForUpdate(Long sensorTypeId) {
		return sensorTypeRepository.findByIdForUpdate(sensorTypeId)
				.orElseThrow(() -> new SensorEnvException(SensorEnvErrorCode.SENSOR_TYPE_NOT_FOUND,
						"传感器类型不存在，sensorTypeId=" + sensorTypeId));
	}

	private String requireSensorTypeName(Long sensorTypeId) {
		Map<Long, String> typeNames = sensorTypeService.listAll().stream()
				.collect(Collectors.toMap(item -> item.getId(), item -> item.getSensorTypeName()));
		return requireSensorTypeName(sensorTypeId, typeNames);
	}

	private String requireSensorTypeName(Long sensorTypeId, Map<Long, String> typeNames) {
		String name = typeNames.get(sensorTypeId);
		if (name == null) {
			throw new IllegalStateException("传感器引用的类型不存在，sensorTypeId=" + sensorTypeId);
		}
		return name;
	}

	private void bindTarget(Long sensorId, SensorTargetType targetType, Long targetId) {
		try {
			switch (targetType) {
				case LAB -> labSensorBindingService.bind(sensorId, targetId);
				case DEVICE -> deviceSensorBindingService.bind(sensorId, targetId);
			}
		}
		catch (LabManagementException ex) {
			throw translateLabBindingException(ex, targetId);
		}
		catch (EquipmentException ex) {
			throw translateDeviceBindingException(ex, targetId);
		}
	}

	private void unbindTarget(Long sensorId, SensorTargetType targetType, Long targetId) {
		try {
			switch (targetType) {
				case LAB -> labSensorBindingService.unbind(sensorId, targetId);
				case DEVICE -> deviceSensorBindingService.unbind(sensorId, targetId);
			}
		}
		catch (LabManagementException ex) {
			throw translateLabBindingException(ex, targetId);
		}
		catch (EquipmentException ex) {
			throw translateDeviceBindingException(ex, targetId);
		}
	}

	private RuntimeException translateLabBindingException(LabManagementException ex, Long targetId) {
		if (ex.getCode() == LabManagementErrorCode.LAB_NOT_FOUND.getCode()) {
			return targetNotFound(SensorTargetType.LAB, targetId);
		}
		if (ex.getCode() == LabManagementErrorCode.LAB_SENSOR_BINDING_CONFLICT.getCode()) {
			return bindingConflict(ex.getMessage());
		}
		return ex;
	}

	private RuntimeException translateDeviceBindingException(EquipmentException ex, Long targetId) {
		if (ex.getCode() == EquipmentErrorCode.DEVICE_NOT_FOUND.getCode()) {
			return targetNotFound(SensorTargetType.DEVICE, targetId);
		}
		if (ex.getCode() == EquipmentErrorCode.DEVICE_SENSOR_BINDING_CONFLICT.getCode()) {
			return bindingConflict(ex.getMessage());
		}
		return ex;
	}

	private SensorEnvException targetNotFound(SensorTargetType targetType, Long targetId) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_TARGET_NOT_FOUND,
				"传感器绑定目标不存在，targetType=" + targetType.getDescription() + ", targetId=" + targetId);
	}

	private SensorEnvException bindingConflict(String message) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_BINDING_CONFLICT, message);
	}

	private SensorEnvException sensorNotFound(Long sensorId) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_NOT_FOUND,
				"传感器不存在或已删除，sensorId=" + sensorId);
	}

	private SensorEnvException duplicateSensorNo(String sensorNo) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_NO_DUPLICATE,
				"传感器编号已存在，sensorNo=" + sensorNo);
	}

	private SensorEnvException invalidParam(String message) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_ENV_PARAM_INVALID, message);
	}

	private void evictAfterCommit(String sensorNo) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			cacheManager.evict(sensorNo);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cacheManager.evict(sensorNo);
			}
		});
	}
}
