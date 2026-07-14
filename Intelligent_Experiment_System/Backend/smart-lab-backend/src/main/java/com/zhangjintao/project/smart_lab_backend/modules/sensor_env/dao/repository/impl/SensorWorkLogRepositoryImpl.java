package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorWorkLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper.SensorWorkLogMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorWorkLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorWorkLogQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * 传感器工作日志 Repository 实现。
 * <p>
 * detectedData 字段在数据库中以 JSON 字符串存储，读写时在此层完成序列化/反序列化。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Repository
@RequiredArgsConstructor
public class SensorWorkLogRepositoryImpl implements SensorWorkLogRepository {

	private static final TypeReference<Map<String, BigDecimal>> DETECTED_DATA_TYPE = new TypeReference<>() {
	};

	private final SensorWorkLogMapper sensorWorkLogMapper;
	private final ObjectMapper objectMapper;

	@Override
	public SensorWorkLog save(SensorWorkLog workLog) {
		SensorWorkLogDO dataObject = SensorEnvPersistenceConverter.toSensorWorkLogDO(workLog);
		dataObject.setDetectedData(toJson(workLog.getDetectedData()));
		sensorWorkLogMapper.insert(dataObject);
		return findById(dataObject.getId())
				.orElseThrow(() -> new IllegalStateException("工作日志新增后无法查询，id=" + dataObject.getId()));
	}

	@Override
	public Optional<SensorWorkLog> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sensorWorkLogMapper.selectById(id))
				.map(this::buildEntity);
	}

	@Override
	public PageResult<SensorWorkLog> pageBySensorIdAndTime(SensorWorkLogQueryCriteria criteria) {
		SensorWorkLogQueryCriteria safe = criteria == null
				? SensorWorkLogQueryCriteria.builder().build() : criteria;
		long pageNum = safe.getPage() == null ? SensorEnvConstants.DEFAULT_PAGE : safe.getPage();
		long pageSize = safe.getPageSize() == null ? SensorEnvConstants.DEFAULT_PAGE_SIZE : safe.getPageSize();

		LambdaQueryWrapper<SensorWorkLogDO> wrapper = new LambdaQueryWrapper<SensorWorkLogDO>()
				.eq(SensorWorkLogDO::getSensorId, safe.getSensorId())
				.ge(safe.getStartTime() != null, SensorWorkLogDO::getDetectedAt, safe.getStartTime())
				.le(safe.getEndTime() != null, SensorWorkLogDO::getDetectedAt, safe.getEndTime())
				.orderByDesc(SensorWorkLogDO::getDetectedAt)
				.orderByDesc(SensorWorkLogDO::getId);

		Page<SensorWorkLogDO> page = sensorWorkLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
		return PageResult.of(
				page.getRecords().stream().map(this::buildEntity).toList(),
				page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public Optional<SensorWorkLog> findLatestBySensorId(Long sensorId) {
		if (sensorId == null) {
			return Optional.empty();
		}
		Page<SensorWorkLogDO> page = sensorWorkLogMapper.selectPage(
				Page.of(1, 1),
				new LambdaQueryWrapper<SensorWorkLogDO>()
						.eq(SensorWorkLogDO::getSensorId, sensorId)
						.orderByDesc(SensorWorkLogDO::getDetectedAt)
						.orderByDesc(SensorWorkLogDO::getId));
		if (page.getRecords().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(buildEntity(page.getRecords().get(0)));
	}

	// ==================== 内部辅助方法 ====================

	/**
	 * 将 DO 转换为完整的 Entity（包含 JSON 反序列化）。
	 */
	private SensorWorkLog buildEntity(SensorWorkLogDO dataObject) {
		return SensorWorkLog.builder()
				.id(dataObject.getId())
				.sensorId(dataObject.getSensorId())
				.sensorNo(dataObject.getSensorNo())
				.detectedData(parseJson(dataObject.getDetectedData()))
				.status(dataObject.getStatus())
				.detectedAt(dataObject.getDetectedAt())
				.createdAt(dataObject.getCreatedAt())
				.build();
	}

	/**
	 * 将 Map 序列化为 JSON 字符串。
	 */
	private String toJson(Map<String, BigDecimal> detectedData) {
		try {
			return objectMapper.writeValueAsString(detectedData);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("检测数据序列化失败", e);
		}
	}

	/**
	 * 将 JSON 字符串反序列化为 Map。
	 */
	private Map<String, BigDecimal> parseJson(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return Map.copyOf(objectMapper.readValue(json, DETECTED_DATA_TYPE));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("检测数据反序列化失败，json=" + json, e);
		}
	}
}
