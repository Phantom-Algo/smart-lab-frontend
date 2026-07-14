package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;

import java.util.List;
import java.util.Optional;

public interface SensorTypeRepository {

	Optional<SensorType> findById(Long id);

	Optional<SensorType> findByIdForUpdate(Long id);

	List<SensorType> listAll();

	boolean existsByName(String sensorTypeName, Long excludeId);

	SensorType save(SensorType sensorType);

	SensorType update(SensorType sensorType);

	void deleteById(Long id);
}
