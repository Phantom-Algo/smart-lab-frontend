package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorRepository {

	boolean existsBySensorTypeId(Long sensorTypeId);

	boolean existsBySensorNoIncludingDeleted(String sensorNo);

	Optional<Sensor> findById(Long id);

	Optional<Sensor> findByIdForUpdate(Long id);

	Optional<Sensor> findBySensorNo(String sensorNo);

	Optional<Sensor> findBySensorNoForUpdate(String sensorNo);

	Sensor save(Sensor sensor);

	Sensor updateInfo(Sensor sensor);

	void updateRuntime(Sensor sensor);

	void softDelete(Sensor sensor);

	PageResult<Sensor> page(SensorQueryCriteria criteria);

	List<Sensor> listOfflineCandidates(LocalDateTime cutoff, int limit);

	boolean updateStatusIfCurrent(Long sensorId, SensorRunningStatus currentStatus,
			SensorRunningStatus targetStatus);
}
