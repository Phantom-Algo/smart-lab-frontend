package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorWorkLogQueryCriteria;

import java.util.Optional;

/**
 * 传感器工作日志 Repository 接口。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
public interface SensorWorkLogRepository {

	/**
	 * 插入一条工作日志，返回包含自增ID的实体。
	 */
	SensorWorkLog save(SensorWorkLog workLog);

	/**
	 * 按主键查询日志。
	 */
	Optional<SensorWorkLog> findById(Long id);

	/**
	 * 按传感器ID和时间范围分页查询，按 detected_at DESC, id DESC 排序。
	 */
	PageResult<SensorWorkLog> pageBySensorIdAndTime(SensorWorkLogQueryCriteria criteria);

	/**
	 * 查询指定传感器最新一条工作日志，无记录返回空。
	 */
	Optional<SensorWorkLog> findLatestBySensorId(Long sensorId);
}
