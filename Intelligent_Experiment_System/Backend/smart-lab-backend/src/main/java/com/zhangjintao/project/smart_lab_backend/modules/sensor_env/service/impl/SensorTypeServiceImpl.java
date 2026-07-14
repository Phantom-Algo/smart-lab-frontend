package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorDataTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeCatalogCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorTypeServiceImpl implements SensorTypeService {

	private final SensorTypeRepository sensorTypeRepository;
	private final SensorDataTypeRepository sensorDataTypeRepository;
	private final SensorRepository sensorRepository;
	private final SensorTypeCatalogCacheManager cacheManager;

	@Override
	@Transactional
	public SensorTypeDetailResponse create(SensorTypeCreateCommand command) {
		SensorTypeCreateCommand normalizedCommand = SensorTypeCreateCommand.builder()
				.sensorTypeName(normalizeSensorTypeName(command.getSensorTypeName()))
				.description(normalizeDescription(command.getDescription()))
				.build();
		validateUniqueSensorTypeName(normalizedCommand.getSensorTypeName(), null);
		SensorType saved = sensorTypeRepository.save(SensorType.create(normalizedCommand));
		evictCatalogAfterCommit();
		return SensorEnvConverter.toDetailResponse(saved);
	}

	@Override
	public List<SensorTypeListItemResponse> listAll() {
		return getCatalog().stream()
				.map(SensorEnvConverter::toListItemResponse)
				.toList();
	}

	@Override
	public SensorTypeDetailResponse getById(Long sensorTypeId) {
		SensorType sensorType = getCatalog().stream()
				.filter(item -> item.getId().equals(sensorTypeId))
				.findFirst()
				.orElseThrow(() -> sensorTypeNotFound(sensorTypeId));
		return SensorEnvConverter.toDetailResponse(sensorType);
	}

	@Override
	@Transactional
	public SensorTypeDetailResponse update(Long sensorTypeId, SensorTypeUpdateCommand command) {
		SensorType sensorType = requireSensorTypeForUpdate(sensorTypeId);
		SensorTypeUpdateCommand normalizedCommand = SensorTypeUpdateCommand.builder()
				.sensorTypeName(normalizeSensorTypeName(command.getSensorTypeName()))
				.description(normalizeDescription(command.getDescription()))
				.build();
		validateUniqueSensorTypeName(normalizedCommand.getSensorTypeName(), sensorTypeId);
		sensorType.applyUpdate(normalizedCommand);
		SensorType updated = sensorTypeRepository.update(sensorType);
		updated.setDataTypes(List.copyOf(sensorDataTypeRepository.listBySensorTypeIds(List.of(sensorTypeId))));
		evictCatalogAfterCommit();
		return SensorEnvConverter.toDetailResponse(updated);
	}

	@Override
	@Transactional
	public void delete(Long sensorTypeId) {
		requireSensorTypeForUpdate(sensorTypeId);
		if (sensorRepository.existsBySensorTypeId(sensorTypeId)) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_TYPE_IN_USE,
					"传感器类型已被未删除的传感器实例引用，sensorTypeId=" + sensorTypeId);
		}
		sensorDataTypeRepository.deleteBySensorTypeId(sensorTypeId);
		sensorTypeRepository.deleteById(sensorTypeId);
		evictCatalogAfterCommit();
	}

	@Override
	@Transactional
	public SensorDataTypeResponse createDataType(Long sensorTypeId, SensorDataTypeCreateCommand command) {
		requireSensorTypeForUpdate(sensorTypeId);
		SensorDataTypeCreateCommand normalizedCommand = SensorDataTypeCreateCommand.builder()
				.dataName(normalizeDataName(command.getDataName()))
				.unit(normalizeUnit(command.getUnit()))
				.build();
		validateUniqueDataName(sensorTypeId, normalizedCommand.getDataName(), null);
		SensorDataType saved = sensorDataTypeRepository.save(SensorDataType.create(sensorTypeId, normalizedCommand));
		evictCatalogAfterCommit();
		return SensorEnvConverter.toDataTypeResponse(saved);
	}

	@Override
	@Transactional
	public SensorDataTypeResponse updateDataType(Long dataTypeId, SensorDataTypeUpdateCommand command) {
		SensorDataType initialDataType = requireSensorDataType(dataTypeId);
		requireSensorTypeForDataTypeUpdate(initialDataType.getSensorTypeId(), dataTypeId);
		SensorDataType dataType = requireSensorDataTypeForUpdate(dataTypeId);
		SensorDataTypeUpdateCommand normalizedCommand = SensorDataTypeUpdateCommand.builder()
				.dataName(normalizeDataName(command.getDataName()))
				.unit(normalizeUnit(command.getUnit()))
				.build();
		validateUniqueDataName(dataType.getSensorTypeId(), normalizedCommand.getDataName(), dataTypeId);
		dataType.applyUpdate(normalizedCommand);
		SensorDataType updated = sensorDataTypeRepository.update(dataType);
		evictCatalogAfterCommit();
		return SensorEnvConverter.toDataTypeResponse(updated);
	}

	@Override
	@Transactional
	public void deleteDataType(Long dataTypeId) {
		SensorDataType initialDataType = requireSensorDataType(dataTypeId);
		requireSensorTypeForDataTypeUpdate(initialDataType.getSensorTypeId(), dataTypeId);
		requireSensorDataTypeForUpdate(dataTypeId);
		sensorDataTypeRepository.deleteById(dataTypeId);
		evictCatalogAfterCommit();
	}

	private List<SensorType> getCatalog() {
		return cacheManager.listAll(this::loadCatalog);
	}

	private List<SensorType> loadCatalog() {
		List<SensorType> sensorTypes = sensorTypeRepository.listAll();
		if (sensorTypes.isEmpty()) {
			return List.of();
		}
		Map<Long, List<SensorDataType>> dataTypesBySensorTypeId = sensorDataTypeRepository
				.listBySensorTypeIds(sensorTypes.stream().map(SensorType::getId).toList())
				.stream()
				.collect(Collectors.groupingBy(SensorDataType::getSensorTypeId));
		sensorTypes.forEach(sensorType -> sensorType.setDataTypes(List.copyOf(
				dataTypesBySensorTypeId.getOrDefault(sensorType.getId(), List.of()))));
		return List.copyOf(sensorTypes);
	}

	private SensorType requireSensorTypeForUpdate(Long sensorTypeId) {
		return sensorTypeRepository.findByIdForUpdate(sensorTypeId)
				.orElseThrow(() -> sensorTypeNotFound(sensorTypeId));
	}

	private void requireSensorTypeForDataTypeUpdate(Long sensorTypeId, Long dataTypeId) {
		if (sensorTypeRepository.findByIdForUpdate(sensorTypeId).isEmpty()) {
			throw sensorDataTypeNotFound(dataTypeId);
		}
	}

	private SensorDataType requireSensorDataType(Long dataTypeId) {
		return sensorDataTypeRepository.findById(dataTypeId)
				.orElseThrow(() -> sensorDataTypeNotFound(dataTypeId));
	}

	private SensorDataType requireSensorDataTypeForUpdate(Long dataTypeId) {
		return sensorDataTypeRepository.findByIdForUpdate(dataTypeId)
				.orElseThrow(() -> sensorDataTypeNotFound(dataTypeId));
	}

	private String normalizeSensorTypeName(String sensorTypeName) {
		if (!StringUtils.hasText(sensorTypeName)) {
			throw invalidParam("传感器类型名称不能为空");
		}
		String normalized = sensorTypeName.trim();
		if (normalized.length() > 64) {
			throw invalidParam("传感器类型名称长度不能超过64");
		}
		return normalized;
	}

	private String normalizeDescription(String description) {
		if (!StringUtils.hasText(description)) {
			return null;
		}
		String normalized = description.trim();
		if (normalized.length() > 255) {
			throw invalidParam("传感器类型描述长度不能超过255");
		}
		return normalized;
	}

	private String normalizeDataName(String dataName) {
		if (!StringUtils.hasText(dataName)) {
			throw invalidParam("检测数据名称不能为空");
		}
		String normalized = dataName.trim();
		if (!SensorEnvConstants.DATA_NAME_PATTERN.matcher(normalized).matches()) {
			throw invalidParam("检测数据名称必须使用lower_snake_case");
		}
		return normalized;
	}

	private String normalizeUnit(String unit) {
		if (!StringUtils.hasText(unit)) {
			throw invalidParam("检测数据单位不能为空");
		}
		String normalized = unit.trim();
		if (normalized.length() > 16) {
			throw invalidParam("检测数据单位长度不能超过16");
		}
		return normalized;
	}

	private void validateUniqueSensorTypeName(String sensorTypeName, Long excludeId) {
		if (sensorTypeRepository.existsByName(sensorTypeName, excludeId)) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_TYPE_NAME_DUPLICATE,
					"传感器类型名称重复，sensorTypeName=" + sensorTypeName);
		}
	}

	private void validateUniqueDataName(Long sensorTypeId, String dataName, Long excludeId) {
		if (sensorDataTypeRepository.existsByName(sensorTypeId, dataName, excludeId)) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_TYPE_NAME_DUPLICATE,
					"同一传感器类型下检测数据名称重复，sensorTypeId=" + sensorTypeId
							+ ", dataName=" + dataName);
		}
	}

	private SensorEnvException sensorTypeNotFound(Long sensorTypeId) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_TYPE_NOT_FOUND,
				"传感器类型不存在，sensorTypeId=" + sensorTypeId);
	}

	private SensorEnvException sensorDataTypeNotFound(Long dataTypeId) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_TYPE_NOT_FOUND,
				"检测数据项不存在，dataTypeId=" + dataTypeId);
	}

	private SensorEnvException invalidParam(String message) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_ENV_PARAM_INVALID, message);
	}

	private void evictCatalogAfterCommit() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			cacheManager.evictAll();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cacheManager.evictAll();
			}
		});
	}
}
