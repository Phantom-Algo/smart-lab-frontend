package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper.SensorMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SensorRepositoryImpl implements SensorRepository {

	private final SensorMapper sensorMapper;

	@Override
	public boolean existsBySensorTypeId(Long sensorTypeId) {
		if (sensorTypeId == null) {
			return false;
		}
		return sensorMapper.selectCount(new LambdaQueryWrapper<SensorDO>()
				.eq(SensorDO::getSensorTypeId, sensorTypeId)
				.isNull(SensorDO::getDeletedAt)) > 0;
	}

	@Override
	public boolean existsBySensorNoIncludingDeleted(String sensorNo) {
		return StringUtils.hasText(sensorNo) && sensorMapper.selectCount(new LambdaQueryWrapper<SensorDO>()
				.eq(SensorDO::getSensorNo, sensorNo)) > 0;
	}

	@Override
	public Optional<Sensor> findById(Long id) {
		return findOne(id, false);
	}

	@Override
	public Optional<Sensor> findByIdForUpdate(Long id) {
		return findOne(id, true);
	}

	@Override
	public Optional<Sensor> findBySensorNo(String sensorNo) {
		return findBySensorNo(sensorNo, false);
	}

	@Override
	public Optional<Sensor> findBySensorNoForUpdate(String sensorNo) {
		return findBySensorNo(sensorNo, true);
	}

	@Override
	public Sensor save(Sensor sensor) {
		SensorDO dataObject = SensorEnvPersistenceConverter.toSensorDO(sensor);
		try {
			sensorMapper.insert(dataObject);
		}
		catch (DuplicateKeyException ex) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_NO_DUPLICATE,
					"传感器编号已存在，sensorNo=" + sensor.getSensorNo());
		}
		return findById(dataObject.getId())
				.orElseThrow(() -> new IllegalStateException("传感器新增后无法查询，id=" + dataObject.getId()));
	}

	@Override
	public Sensor updateInfo(Sensor sensor) {
		int affectedRows = sensorMapper.update(null, new LambdaUpdateWrapper<SensorDO>()
				.eq(SensorDO::getId, sensor.getId())
				.isNull(SensorDO::getDeletedAt)
				.set(SensorDO::getSensorTypeId, sensor.getSensorTypeId())
				.set(SensorDO::getTargetType, sensor.getTargetType().getCode())
				.set(SensorDO::getTargetId, sensor.getTargetId())
				.set(SensorDO::getRemark, sensor.getRemark()));
		requireSingleRow(affectedRows, "更新传感器信息", sensor.getId());
		return findById(sensor.getId())
				.orElseThrow(() -> new IllegalStateException("传感器更新后无法查询，id=" + sensor.getId()));
	}

	@Override
	public void updateRuntime(Sensor sensor) {
		int affectedRows = sensorMapper.update(null, new LambdaUpdateWrapper<SensorDO>()
				.eq(SensorDO::getId, sensor.getId())
				.isNull(SensorDO::getDeletedAt)
				.set(SensorDO::getRunningStatus, sensor.getRunningStatus().getCode())
				.set(SensorDO::getLastReportAt, sensor.getLastReportAt()));
		requireSingleRow(affectedRows, "更新传感器运行状态", sensor.getId());
	}

	@Override
	public void softDelete(Sensor sensor) {
		int affectedRows = sensorMapper.update(null, new LambdaUpdateWrapper<SensorDO>()
				.eq(SensorDO::getId, sensor.getId())
				.isNull(SensorDO::getDeletedAt)
				.set(SensorDO::getDeletedAt, sensor.getDeletedAt()));
		requireSingleRow(affectedRows, "软删除传感器", sensor.getId());
	}

	@Override
	public PageResult<Sensor> page(SensorQueryCriteria criteria) {
		SensorQueryCriteria safe = criteria == null ? SensorQueryCriteria.builder().build() : criteria;
		long pageNum = safe.getPage() == null ? SensorEnvConstants.DEFAULT_PAGE : safe.getPage();
		long pageSize = safe.getPageSize() == null ? SensorEnvConstants.DEFAULT_PAGE_SIZE : safe.getPageSize();
		Page<SensorDO> page = sensorMapper.selectPage(Page.of(pageNum, pageSize), buildPageWrapper(safe));
		return PageResult.of(page.getRecords().stream().map(SensorEnvPersistenceConverter::toSensor).toList(),
				page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public List<Sensor> listOfflineCandidates(LocalDateTime cutoff, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 1000));
		return sensorMapper.selectList(new LambdaQueryWrapper<SensorDO>()
				.eq(SensorDO::getRunningStatus, SensorRunningStatus.NORMAL.getCode())
				.isNull(SensorDO::getDeletedAt)
				.and(wrapper -> wrapper
						.and(reported -> reported.isNotNull(SensorDO::getLastReportAt)
								.lt(SensorDO::getLastReportAt, cutoff))
						.or(notReported -> notReported.isNull(SensorDO::getLastReportAt)
								.lt(SensorDO::getCreatedAt, cutoff)))
				.orderByAsc(SensorDO::getId)
				.last("LIMIT " + safeLimit))
				.stream()
				.map(SensorEnvPersistenceConverter::toSensor)
				.toList();
	}

	@Override
	public boolean updateStatusIfCurrent(Long sensorId, SensorRunningStatus currentStatus,
			SensorRunningStatus targetStatus) {
		return sensorMapper.update(null, new LambdaUpdateWrapper<SensorDO>()
				.eq(SensorDO::getId, sensorId)
				.eq(SensorDO::getRunningStatus, currentStatus.getCode())
				.isNull(SensorDO::getDeletedAt)
				.set(SensorDO::getRunningStatus, targetStatus.getCode())) == 1;
	}

	private Optional<Sensor> findOne(Long id, boolean forUpdate) {
		if (id == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<SensorDO> wrapper = new LambdaQueryWrapper<SensorDO>()
				.eq(SensorDO::getId, id)
				.isNull(SensorDO::getDeletedAt)
				.last(forUpdate ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
		return Optional.ofNullable(sensorMapper.selectOne(wrapper)).map(SensorEnvPersistenceConverter::toSensor);
	}

	private Optional<Sensor> findBySensorNo(String sensorNo, boolean forUpdate) {
		if (!StringUtils.hasText(sensorNo)) {
			return Optional.empty();
		}
		LambdaQueryWrapper<SensorDO> wrapper = new LambdaQueryWrapper<SensorDO>()
				.eq(SensorDO::getSensorNo, sensorNo)
				.isNull(SensorDO::getDeletedAt)
				.last(forUpdate ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
		return Optional.ofNullable(sensorMapper.selectOne(wrapper)).map(SensorEnvPersistenceConverter::toSensor);
	}

	private LambdaQueryWrapper<SensorDO> buildPageWrapper(SensorQueryCriteria criteria) {
		LambdaQueryWrapper<SensorDO> wrapper = new LambdaQueryWrapper<SensorDO>().isNull(SensorDO::getDeletedAt);
		wrapper.like(StringUtils.hasText(criteria.getKeyword()), SensorDO::getSensorNo,
				StringUtils.hasText(criteria.getKeyword()) ? criteria.getKeyword().trim() : null);
		wrapper.eq(criteria.getSensorTypeId() != null, SensorDO::getSensorTypeId, criteria.getSensorTypeId());
		wrapper.eq(criteria.getTargetType() != null, SensorDO::getTargetType, criteria.getTargetType());
		wrapper.eq(criteria.getRunningStatus() != null, SensorDO::getRunningStatus, criteria.getRunningStatus());
		applyOrder(wrapper, criteria.getSortField(), criteria.getSortOrder());
		return wrapper;
	}

	private void applyOrder(LambdaQueryWrapper<SensorDO> wrapper, String sortField, String sortOrder) {
		boolean asc = "asc".equalsIgnoreCase(sortOrder);
		ResolvedSortColumn resolved = resolveSortColumn(sortField);
		wrapper.orderBy(true, asc, resolved.column());
		if (!resolved.idColumn()) {
			wrapper.orderByDesc(SensorDO::getId);
		}
	}

	private ResolvedSortColumn resolveSortColumn(String sortField) {
		String normalized = StringUtils.hasText(sortField) ? sortField.trim() : "created_at";
		normalized = normalized.replace("-", "_").toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "id" -> new ResolvedSortColumn(SensorDO::getId, true);
			case "sensor_no", "sensorno" -> new ResolvedSortColumn(SensorDO::getSensorNo, false);
			case "running_status", "runningstatus" -> new ResolvedSortColumn(SensorDO::getRunningStatus, false);
			case "last_report_at", "lastreportat" -> new ResolvedSortColumn(SensorDO::getLastReportAt, false);
			case "created_at", "createdat" -> new ResolvedSortColumn(SensorDO::getCreatedAt, false);
			case "updated_at", "updatedat" -> new ResolvedSortColumn(SensorDO::getUpdatedAt, false);
			default -> throw new SensorEnvException(SensorEnvErrorCode.SENSOR_SORT_FIELD_INVALID,
					"不支持的传感器排序字段：" + sortField);
		};
	}

	private void requireSingleRow(int affectedRows, String operation, Long sensorId) {
		if (affectedRows != 1) {
			throw new IllegalStateException(operation + "行数异常，sensorId=" + sensorId + ", affectedRows=" + affectedRows);
		}
	}

	private record ResolvedSortColumn(SFunction<SensorDO, ?> column, boolean idColumn) {
	}
}
