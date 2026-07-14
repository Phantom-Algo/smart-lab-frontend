package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SensorDataTypeRepository {

	Optional<SensorDataType> findById(Long id);

	Optional<SensorDataType> findByIdForUpdate(Long id);

	List<SensorDataType> listBySensorTypeIds(Collection<Long> sensorTypeIds);

	boolean existsByName(Long sensorTypeId, String dataName, Long excludeId);

	SensorDataType save(SensorDataType sensorDataType);

	SensorDataType update(SensorDataType sensorDataType);

	void deleteById(Long id);

	void deleteBySensorTypeId(Long sensorTypeId);
}
