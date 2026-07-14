package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorDataTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper.SensorDataTypeMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorDataTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SensorDataTypeRepositoryImpl implements SensorDataTypeRepository {

	private final SensorDataTypeMapper sensorDataTypeMapper;

	@Override
	public Optional<SensorDataType> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sensorDataTypeMapper.selectById(id))
				.map(SensorEnvPersistenceConverter::toSensorDataType);
	}

	@Override
	public Optional<SensorDataType> findByIdForUpdate(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sensorDataTypeMapper.selectOne(new LambdaQueryWrapper<SensorDataTypeDO>()
					.eq(SensorDataTypeDO::getId, id)
					.last("FOR UPDATE")))
				.map(SensorEnvPersistenceConverter::toSensorDataType);
	}

	@Override
	public List<SensorDataType> listBySensorTypeIds(Collection<Long> sensorTypeIds) {
		if (sensorTypeIds == null || sensorTypeIds.isEmpty()) {
			return List.of();
		}
		return sensorDataTypeMapper.selectList(new LambdaQueryWrapper<SensorDataTypeDO>()
					.in(SensorDataTypeDO::getSensorTypeId, sensorTypeIds)
					.orderByAsc(SensorDataTypeDO::getId))
				.stream()
				.map(SensorEnvPersistenceConverter::toSensorDataType)
				.toList();
	}

	@Override
	public boolean existsByName(Long sensorTypeId, String dataName, Long excludeId) {
		if (sensorTypeId == null || !StringUtils.hasText(dataName)) {
			return false;
		}
		LambdaQueryWrapper<SensorDataTypeDO> wrapper = new LambdaQueryWrapper<SensorDataTypeDO>()
				.eq(SensorDataTypeDO::getSensorTypeId, sensorTypeId)
				.eq(SensorDataTypeDO::getDataName, dataName);
		if (excludeId != null) {
			wrapper.ne(SensorDataTypeDO::getId, excludeId);
		}
		return sensorDataTypeMapper.selectCount(wrapper) > 0;
	}

	@Override
	public SensorDataType save(SensorDataType sensorDataType) {
		SensorDataTypeDO dataObject = SensorEnvPersistenceConverter.toSensorDataTypeDO(sensorDataType);
		try {
			sensorDataTypeMapper.insert(dataObject);
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(sensorDataType.getSensorTypeId(), sensorDataType.getDataName());
		}
		return findById(dataObject.getId())
				.orElseThrow(() -> new IllegalStateException("检测数据项新增后无法查询，id=" + dataObject.getId()));
	}

	@Override
	public SensorDataType update(SensorDataType sensorDataType) {
		try {
			SensorDataTypeDO dataObject = SensorEnvPersistenceConverter.toSensorDataTypeDO(sensorDataType);
			// 创建时间不可修改，更新时间交由 MySQL 的 ON UPDATE CURRENT_TIMESTAMP 维护。
			dataObject.setCreatedAt(null);
			dataObject.setUpdatedAt(null);
			int affectedRows = sensorDataTypeMapper.updateById(dataObject);
			if (affectedRows != 1) {
				throw new IllegalStateException("检测数据项更新行数异常，id=" + sensorDataType.getId());
			}
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(sensorDataType.getSensorTypeId(), sensorDataType.getDataName());
		}
		return findById(sensorDataType.getId())
				.orElseThrow(() -> new IllegalStateException("检测数据项更新后无法查询，id=" + sensorDataType.getId()));
	}

	@Override
	public void deleteById(Long id) {
		int affectedRows = sensorDataTypeMapper.deleteById(id);
		if (affectedRows != 1) {
			throw new IllegalStateException("检测数据项删除行数异常，id=" + id);
		}
	}

	@Override
	public void deleteBySensorTypeId(Long sensorTypeId) {
		sensorDataTypeMapper.delete(new LambdaQueryWrapper<SensorDataTypeDO>()
				.eq(SensorDataTypeDO::getSensorTypeId, sensorTypeId));
	}

	private SensorEnvException duplicateNameException(Long sensorTypeId, String dataName) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_TYPE_NAME_DUPLICATE,
				"同一传感器类型下检测数据名称重复，sensorTypeId=" + sensorTypeId + ", dataName=" + dataName);
	}
}
