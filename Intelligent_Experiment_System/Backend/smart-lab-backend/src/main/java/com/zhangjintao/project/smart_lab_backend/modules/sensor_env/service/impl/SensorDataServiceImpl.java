package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorWorkLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataReceiveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorWorkLogQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataReceiveResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorWorkLogResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataRouteService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorRuntimeService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeCatalogCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 传感器数据服务实现。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>接收外部模拟程序推送的传感器检测数据，完成校验和持久化</li>
 *   <li>在同一事务中更新传感器心跳（复用模块二的 {@link SensorRuntimeService}）并写入工作日志</li>
 *   <li>提供历史数据和最新数据的查询</li>
 * </ul>
 *
 * 告警判定不在本模块范围——状态统一写 1（正常），不返回 {@code alarmTriggered}。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

	/** 检测时间允许的时钟偏差上限（分钟） */
	private static final long CLOCK_SKEW_MINUTES = 5;

	private final SensorRuntimeService sensorRuntimeService;
	private final SensorWorkLogRepository sensorWorkLogRepository;
	private final SensorRepository sensorRepository;
	private final SensorTypeCatalogCacheManager typeCatalogCacheManager;
	private final SensorDataRouteService sensorDataRouteService;

	@Override
	@Transactional
	public SensorDataReceiveResponse receive(SensorDataReceiveCommand command) {
		// 1. 规范化并校验传感器编号
		String sensorNo = normalizeSensorNo(command.getSensorNo());

		// 2. 查询传感器运行快照（含存在性校验、状态校验）
		SensorRuntimeSnapshot snapshot = sensorRuntimeService.getBySensorNo(sensorNo);
		// 故障传感器拒绝上报
		if (Integer.valueOf(SensorRunningStatus.FAULT.getCode()).equals(snapshot.getRunningStatus())) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_STATUS_TRANSITION_INVALID,
					"传感器处于故障状态，不允许上报数据，sensorNo=" + sensorNo);
		}

		// 3. 校验 detectedData 字段名是否在 sensor_data_type 中注册，并取得类型元数据
		SensorType sensorType = resolveAndValidateDetectedDataFields(
				snapshot.getSensorTypeId(), command.getDetectedData());

		// 4. 校验并解析检测时间
		LocalDateTime detectedAt = parseAndValidateDetectedAt(command.getDetectedAt());

		// 5. 在同一事务中：更新心跳 + 写入工作日志
		sensorRuntimeService.recordSuccessfulReport(sensorNo, detectedAt);
		SensorWorkLog workLog = SensorWorkLog.create(
				snapshot.getSensorId(), sensorNo, command.getDetectedData(), detectedAt);
		SensorWorkLog saved = sensorWorkLogRepository.save(workLog);
		sensorDataRouteService.publishAfterCommit(SensorDataRouteCommand.builder()
				.logId(saved.getId())
				.sensorId(snapshot.getSensorId())
				.sensorNo(sensorNo)
				.sensorTypeId(snapshot.getSensorTypeId())
				.sensorTypeName(sensorType.getSensorTypeName())
				.targetType(snapshot.getTargetType())
				.targetId(snapshot.getTargetId())
				.detectedData(buildDetectedDataWithUnit(sensorType, command.getDetectedData()))
				.detectedAt(detectedAt)
				.build());

		log.info("传感器数据接收成功，sensorNo={}, logId={}, dataFields={}",
				sensorNo, saved.getId(), command.getDetectedData().keySet());

		// 6. 返回结果（不包含 alarmTriggered）
		return SensorDataReceiveResponse.builder()
				.logId(saved.getId())
				.status(saved.getStatus())
				.build();
	}

	@Override
	public PageResult<SensorWorkLogResponse> queryLogs(SensorWorkLogQueryCriteria criteria) {
		SensorWorkLogQueryCriteria safe = normalizeCriteria(criteria);
		// 校验传感器存在且未删除
		requireSensorExists(safe.getSensorId());
		PageResult<SensorWorkLog> page = sensorWorkLogRepository.pageBySensorIdAndTime(safe);
		return PageResult.of(
				page.getRecords().stream().map(SensorEnvConverter::toWorkLogResponse).toList(),
				page.getTotal(), page.getPageNum(), page.getPageSize());
	}

	@Override
	public SensorWorkLogResponse getLatestLog(Long sensorId) {
		// 校验传感器存在且未删除
		requireSensorExists(sensorId);
		return sensorWorkLogRepository.findLatestBySensorId(sensorId)
				.map(SensorEnvConverter::toWorkLogResponse)
				.orElse(null);
	}

	// ==================== 内部校验方法 ====================

	/**
	 * 规范化传感器编号。
	 */
	private String normalizeSensorNo(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw invalidParam("传感器编号不能为空");
		}
		String trimmed = raw.trim();
		if (trimmed.length() > 64) {
			throw invalidParam("传感器编号长度不能超过64");
		}
		return trimmed;
	}

	/**
	 * 校验 detectedData 的 key 必须是传感器类型已注册 data_name 的子集。
	 * 未注册的字段直接拒绝，不静默忽略。
	 */
	private SensorType resolveAndValidateDetectedDataFields(Long sensorTypeId, Map<String, BigDecimal> detectedData) {
		if (detectedData == null || detectedData.isEmpty()) {
			throw invalidParam("检测数据不能为空");
		}
		if (detectedData.keySet().stream().anyMatch(key -> !StringUtils.hasText(key))) {
			throw invalidParam("检测数据字段名不能为空");
		}

		// 从类型目录缓存获取该传感器类型的全部注册数据项
		List<SensorType> catalog = typeCatalogCacheManager.listAll(() -> {
			throw new IllegalStateException("类型目录缓存未命中且未提供数据库加载器");
		});
		SensorType sensorType = catalog.stream()
				.filter(t -> t.getId().equals(sensorTypeId))
				.findFirst()
				.orElse(null);

		if (sensorType == null || sensorType.getDataTypes() == null || sensorType.getDataTypes().isEmpty()) {
			throw invalidParam("传感器类型未配置检测数据项，sensorTypeId=" + sensorTypeId);
		}

		Set<String> registeredNames = sensorType.getDataTypes().stream()
				.map(SensorDataType::getDataName)
				.collect(Collectors.toSet());

		// 找出未注册的字段名
		List<String> unknownFields = detectedData.keySet().stream()
				.map(String::trim)
				.filter(key -> !registeredNames.contains(key))
				.toList();

		if (!unknownFields.isEmpty()) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_DATA_FIELD_MISMATCH,
					"上报数据包含未注册的字段：" + String.join(", ", unknownFields)
					+ "，sensorTypeId=" + sensorTypeId);
		}
		return sensorType;
	}

	/**
	 * 按消息契约组装“数值 + 单位”的检测数据快照。
	 */
	private Map<String, SensorDataMessage.DataItem> buildDetectedDataWithUnit(
			SensorType sensorType, Map<String, BigDecimal> detectedData) {
		Map<String, String> unitsByName = sensorType.getDataTypes().stream()
				.collect(Collectors.toMap(SensorDataType::getDataName, SensorDataType::getUnit));
		Map<String, SensorDataMessage.DataItem> routedData = new LinkedHashMap<>();
		for (Map.Entry<String, BigDecimal> entry : detectedData.entrySet()) {
			String dataName = entry.getKey().trim();
			if (routedData.containsKey(dataName)) {
				throw invalidParam("检测数据字段重复：" + dataName);
			}
			routedData.put(dataName, SensorDataMessage.DataItem.builder()
					.value(entry.getValue())
					.unit(unitsByName.get(dataName))
					.build());
		}
		return Collections.unmodifiableMap(routedData);
	}

	/**
	 * 解析并校验 detectedAt。
	 * <ul>
	 *   <li>不传 → 使用服务端当前时间</li>
	 *   <li>晚于 now + 5分钟 → 拒绝（时钟偏差超出容差）</li>
	 * </ul>
	 */
	private LocalDateTime parseAndValidateDetectedAt(String detectedAtStr) {
		LocalDateTime now = LocalDateTime.now();
		if (!StringUtils.hasText(detectedAtStr)) {
			return now;
		}
		LocalDateTime detectedAt;
		try {
			detectedAt = LocalDateTime.parse(detectedAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (DateTimeParseException e) {
			throw invalidParam("检测时间格式非法，请使用 ISO 8601 格式，如 2026-07-09T14:30:00");
		}
		// 不允许超过当前时间 + 容差
		LocalDateTime maxAllowed = now.plusMinutes(CLOCK_SKEW_MINUTES);
		if (detectedAt.isAfter(maxAllowed)) {
			throw new SensorEnvException(SensorEnvErrorCode.DETECTED_AT_INVALID,
					"检测时间超出合法范围，detectedAt=" + detectedAtStr
					+ "，允许最晚=" + maxAllowed.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		}
		return detectedAt;
	}

	/**
	 * 规范化查询条件，设置默认值。
	 */
	private SensorWorkLogQueryCriteria normalizeCriteria(SensorWorkLogQueryCriteria criteria) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startTime = criteria.getStartTime() != null ? criteria.getStartTime() : now.minusHours(24);
		LocalDateTime endTime = criteria.getEndTime() != null ? criteria.getEndTime() : now;

		// 校验时间范围
		if (startTime.isAfter(endTime)) {
			throw invalidParam("查询起始时间不能晚于结束时间");
		}
		if (startTime.plusDays(90).isBefore(endTime)) {
			throw invalidParam("查询时间范围不能超过90天");
		}

		long page = criteria.getPage() == null ? SensorEnvConstants.DEFAULT_PAGE : criteria.getPage();
		long pageSize = criteria.getPageSize() == null ? SensorEnvConstants.DEFAULT_PAGE_SIZE : criteria.getPageSize();

		return SensorWorkLogQueryCriteria.builder()
				.sensorId(criteria.getSensorId())
				.startTime(startTime)
				.endTime(endTime)
				.page(page)
				.pageSize(pageSize)
				.build();
	}

	/**
	 * 校验传感器存在且未删除。
	 */
	private void requireSensorExists(Long sensorId) {
		if (sensorId == null || sensorId <= 0) {
			throw invalidParam("传感器ID必须为正数");
		}
		boolean exists = sensorRepository.findById(sensorId).isPresent();
		if (!exists) {
			throw new SensorEnvException(SensorEnvErrorCode.SENSOR_NOT_FOUND,
					"传感器不存在或已删除，sensorId=" + sensorId);
		}
	}

	private SensorEnvException invalidParam(String message) {
		return new SensorEnvException(SensorEnvErrorCode.SENSOR_ENV_PARAM_INVALID, message);
	}
}
