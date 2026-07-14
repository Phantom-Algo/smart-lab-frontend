package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper.SensorTypeMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SensorTypeRepositoryImpl implements SensorTypeRepository {

	private final SensorTypeMapper sensorTypeMapper;

	@Override
	public Optional<SensorType> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sensorTypeMapper.selectById(id))
				.map(SensorEnvPersistenceConverter::toSensorType);
	}

	@Override
	public Optional<SensorType> findByIdForUpdate(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sensorTypeMapper.selectOne(new LambdaQueryWrapper<SensorTypeDO>()
					.eq(SensorTypeDO::getId, id)
					.last("FOR UPDATE")))
				.map(SensorEnvPersistenceConverter::toSensorType);
	}

	@Override
	public List<SensorType> listAll() {
		return sensorTypeMapper.selectList(new LambdaQueryWrapper<SensorTypeDO>()
					.orderByAsc(SensorTypeDO::getId))
				.stream()
				.map(SensorEnvPersistenceConverter::toSensorType)
				.toList();
	}

	@Override
	public boolean existsByName(String sensorTypeName, Long excludeId) {
		if (!StringUtils.hasText(sensorTypeName)) {
			return false;
		}
		LambdaQueryWrapper<SensorTypeDO> wrapper = new LambdaQueryWrapper<SensorTypeDO>()
				.eq(SensorTypeDO::getSensorTypeName, sensorTypeName);
		if (excludeId != null) {
			wrapper.ne(SensorTypeDO::getId, excludeId);
		}
		return sensorTypeMapper.selectCount(wrapper) > 0;
	}

	@Override
	public SensorType save(SensorType sensorType) {
		SensorTypeDO dataObject = SensorEnvPersistenceConverter.toSensorTypeDO(sensorType);
		try {
			sensorTypeMapper.insert(dataObject);
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(sensorType.getSensorTypeName());
		}
		return findById(dataObject.getId())
				.orElseThrow(() -> new IllegalStateException("传感器类型新增后无法查询，id=" + dataObject.getId()));
	}

	@Override
	public SensorType update(SensorType sensorType) {
		try {
			SensorTypeDO dataObject = SensorEnvPersistenceConverter.toSensorTypeDO(sensorType);
			// 创建时间不可修改，更新时间交由 MySQL 的 ON UPDATE CURRENT_TIMESTAMP 维护。
			dataObject.setCreatedAt(null);
			dataObject.setUpdatedAt(null);
			int affectedRows = sensorTypeMapper.updateById(dataObject);
			if (affectedRows != 1) {
				throw new IllegalStateException("传感器类型更新行数异常，id=" + sensorType.getId());
			}
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(sensorType.getSensorTypeName());
		}
		return findById(sensorType.getId())
				.orElseThrow(() -> new IllegalStateException("传感器类型更新后无法查询，id=" + sensorType.getId()));
	}

	@Override
	public void deleteById(Long id) {
		int affectedRows = sensorTypeMapper.deleteById(id);
		if (affectedRows != 1) {
			throw new IllegalStateException("传感器类型删除行数异常，id=" + id);
		}
	}

	private SensorEnvException duplicateNameException(String sensorTypeName) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_TYPE_NAME_DUPLICATE,
				"传感器类型名称重复，sensorTypeName=" + sensorTypeName);
	}
}
