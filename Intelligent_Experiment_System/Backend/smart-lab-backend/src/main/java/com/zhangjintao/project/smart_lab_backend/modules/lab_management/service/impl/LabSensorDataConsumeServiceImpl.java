package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.DetectionIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabDetectionLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionAlertItem;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionEvaluationResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionExceedDirection;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorDataConsumeService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabSensorDataConsumeServiceImpl implements LabSensorDataConsumeService {

	private static final String IOT_SOURCE_SYSTEM = "iot";
	private static final Pattern IOT_EVENT_NO_PATTERN =
			Pattern.compile("^iot\\.sensor\\.data\\.report:\\d{16}$");
	private static final TypeReference<List<DetectionAlertItem>> ALERT_ITEM_LIST_TYPE = new TypeReference<>() {
	};

	private final LabRepository labRepository;
	private final DetectionIndicatorRepository detectionIndicatorRepository;
	private final DetectionIndicatorCacheManager detectionIndicatorCacheManager;
	private final LabDetectionLogRepository labDetectionLogRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public LabSensorDataConsumeResult recordAndEvaluate(SensorDataMessage message) {
		validateMessage(message);
		Lab lab = requireLab(message.getTarget().getTargetId());
		LabDetectionLog existingLog = labDetectionLogRepository.findByEventNo(message.getEventNo()).orElse(null);
		if (existingLog != null) {
			return buildDuplicateResult(message, lab, existingLog);
		}
		List<DetectionIndicator> indicators = listIndicators(lab);
		DetectionEvaluationResult evaluationResult = evaluate(message, indicators);
		LabDetectionLog detectionLog = buildDetectionLog(message, lab, evaluationResult);
		try {
			LabDetectionLog savedLog = labDetectionLogRepository.save(detectionLog);
			return buildResult(message, lab, savedLog, evaluationResult, false);
		}
		catch (DuplicateKeyException ex) {
			LabDetectionLog duplicatedLog = labDetectionLogRepository.findByEventNo(message.getEventNo())
					.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_DETECTION_LOG_NOT_FOUND,
							"检测日志唯一键冲突后未查询到原日志，eventNo=" + message.getEventNo()));
			return buildDuplicateResult(message, lab, duplicatedLog);
		}
	}

	private void validateMessage(SensorDataMessage message) {
		if (message == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "传感器消息不能为空");
		}
		requireText(message.getMessageId(), "messageId不能为空");
		if (!IOT_EVENT_NO_PATTERN.matcher(message.getEventNo() == null ? "" : message.getEventNo()).matches()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"IoT事件编号格式不合法，eventNo=" + message.getEventNo());
		}
		if (message.getOccurredAt() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "occurredAt不能为空");
		}
		if (!IOT_SOURCE_SYSTEM.equals(message.getSourceSystem())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"sourceSystem必须为iot，sourceSystem=" + message.getSourceSystem());
		}
		validateSensor(message.getSensor());
		validateTarget(message.getTarget());
		validateDetectedData(message.getDetectedData());
		if (message.getDetectedAt() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "detectedAt不能为空");
		}
	}

	private void validateSensor(SensorDataMessage.SensorInfo sensor) {
		if (sensor == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "sensor不能为空");
		}
		if (sensor.getSensorId() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "sensor.sensorId不能为空");
		}
		requireText(sensor.getSensorNo(), "sensor.sensorNo不能为空");
		if (sensor.getSensorTypeId() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"sensor.sensorTypeId不能为空");
		}
		requireText(sensor.getSensorTypeName(), "sensor.sensorTypeName不能为空");
	}

	private void validateTarget(SensorDataMessage.TargetInfo target) {
		if (target == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "target不能为空");
		}
		if (!Integer.valueOf(SensorTargetType.LAB.getCode()).equals(target.getTargetType())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"实验室消费者仅处理targetType=2的消息，targetType=" + target.getTargetType());
		}
		if (target.getTargetId() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "target.targetId不能为空");
		}
	}

	private void validateDetectedData(Map<String, SensorDataMessage.DataItem> detectedData) {
		if (detectedData == null || detectedData.isEmpty()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, "detectedData不能为空");
		}
		for (Map.Entry<String, SensorDataMessage.DataItem> entry : detectedData.entrySet()) {
			requireText(entry.getKey(), "detectedData数据项名称不能为空");
			SensorDataMessage.DataItem dataItem = entry.getValue();
			if (dataItem == null) {
				throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
						"detectedData[" + entry.getKey() + "]不能为空");
			}
			if (dataItem.getValue() == null) {
				throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
						"detectedData[" + entry.getKey() + "].value不能为空");
			}
			requireText(dataItem.getUnit(), "detectedData[" + entry.getKey() + "].unit不能为空");
		}
	}

	private void requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, message);
		}
	}

	private Lab requireLab(Long labId) {
		return labRepository.findById(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND,
						"传感器消息关联的实验室不存在，labId=" + labId));
	}

	private List<DetectionIndicator> listIndicators(Lab lab) {
		if (lab.getLabTypeId() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID,
					"实验室未关联实验室类型，labId=" + lab.getId());
		}
		List<DetectionIndicator> indicators = detectionIndicatorCacheManager.listByLabTypeId(lab.getLabTypeId(),
				() -> detectionIndicatorRepository.listByLabTypeId(lab.getLabTypeId()));
		if (indicators == null || indicators.isEmpty()) {
			throw new LabManagementException(LabManagementErrorCode.DETECTION_INDICATOR_NOT_FOUND,
					"实验室类型未配置检测指标，labTypeId=" + lab.getLabTypeId());
		}
		return indicators;
	}

	private DetectionEvaluationResult evaluate(SensorDataMessage message, List<DetectionIndicator> indicators) {
		Map<String, DetectionIndicator> indicatorMap = indicators.stream()
				.filter(indicator -> StringUtils.hasText(indicator.getIndicatorName()))
				.collect(Collectors.toMap(DetectionIndicator::getIndicatorName, Function.identity(),
						(left, right) -> left, LinkedHashMap::new));
		List<DetectionAlertItem> alertItems = new ArrayList<>();
		List<String> unknownIndicatorNames = new ArrayList<>();
		for (Map.Entry<String, SensorDataMessage.DataItem> entry : message.getDetectedData().entrySet()) {
			DetectionIndicator indicator = indicatorMap.get(entry.getKey());
			if (indicator == null) {
				unknownIndicatorNames.add(entry.getKey());
				continue;
			}
			SensorDataMessage.DataItem dataItem = entry.getValue();
			if (!containsSafely(indicator, dataItem.getValue())) {
				alertItems.add(DetectionAlertItem.builder()
						.indicatorName(entry.getKey())
						.actualValue(dataItem.getValue())
						.unit(dataItem.getUnit())
						.safeRange(indicator.getSafeRange().toExternalValue())
						.exceedDirection(resolveExceedDirection(dataItem.getValue(), indicator.getSafeRange()))
						.build());
			}
		}
		return DetectionEvaluationResult.builder()
				.alertItems(List.copyOf(alertItems))
				.unknownIndicatorNames(List.copyOf(unknownIndicatorNames))
				.build();
	}

	private boolean containsSafely(DetectionIndicator indicator, BigDecimal value) {
		try {
			DetectionSafeRange safeRange = indicator.getSafeRange();
			if (safeRange == null) {
				throw new IllegalStateException("安全区间不能为空");
			}
			return safeRange.contains(value);
		}
		catch (IllegalArgumentException | IllegalStateException ex) {
			throw new LabManagementException(LabManagementErrorCode.DETECTION_SAFE_RANGE_INVALID,
					"检测指标安全区间不合法，indicatorName=" + indicator.getIndicatorName() + "，原因=" + ex.getMessage());
		}
	}

	private DetectionExceedDirection resolveExceedDirection(BigDecimal value, DetectionSafeRange safeRange) {
		List<DetectionSafeRangeSegment> segments = safeRange.getSegments();
		boolean allSegmentsHaveMin = segments.stream().allMatch(segment -> segment.getMin() != null);
		if (allSegmentsHaveMin) {
			BigDecimal minBoundary = segments.stream()
					.map(DetectionSafeRangeSegment::getMin)
					.min(Comparator.naturalOrder())
					.orElseThrow();
			if (value.compareTo(minBoundary) < 0) {
				return DetectionExceedDirection.LOWER;
			}
		}
		boolean allSegmentsHaveMax = segments.stream().allMatch(segment -> segment.getMax() != null);
		if (allSegmentsHaveMax) {
			BigDecimal maxBoundary = segments.stream()
					.map(DetectionSafeRangeSegment::getMax)
					.max(Comparator.naturalOrder())
					.orElseThrow();
			if (value.compareTo(maxBoundary) > 0) {
				return DetectionExceedDirection.UPPER;
			}
		}
		return DetectionExceedDirection.OUT_OF_RANGE;
	}

	private LabDetectionLog buildDetectionLog(SensorDataMessage message, Lab lab,
			DetectionEvaluationResult evaluationResult) {
		SensorDataMessage.SensorInfo sensor = message.getSensor();
		return LabDetectionLog.builder()
				.eventNo(message.getEventNo())
				.messageId(message.getMessageId())
				.labId(lab.getId())
				.sensorId(sensor.getSensorId())
				.sensorNo(sensor.getSensorNo())
				.detectedData(writeDetectedDataSnapshot(message, lab, evaluationResult))
				.detectedAt(message.getDetectedAt())
				.status(evaluationResult.hasAlert() ? DetectionStatus.ALERT : DetectionStatus.NORMAL)
				.alertDetail(evaluationResult.hasAlert() ? writeAlertDetail(evaluationResult.getAlertItems()) : null)
				.createdAt(LocalDateTime.now())
				.build();
	}

	private String writeDetectedDataSnapshot(SensorDataMessage message, Lab lab,
			DetectionEvaluationResult evaluationResult) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("messageId", message.getMessageId());
		snapshot.put("eventNo", message.getEventNo());
		snapshot.put("sourceSystem", message.getSourceSystem());
		snapshot.put("occurredAt", message.getOccurredAt());
		snapshot.put("sensor", message.getSensor());
		snapshot.put("target", message.getTarget());
		snapshot.put("labId", lab.getId());
		snapshot.put("labTypeId", lab.getLabTypeId());
		snapshot.put("detectedAt", message.getDetectedAt());
		snapshot.put("detectedData", message.getDetectedData());
		snapshot.put("unknownIndicatorNames", evaluationResult.getUnknownIndicatorNames());
		snapshot.put("alertItems", evaluationResult.getAlertItems());
		return writeJson(snapshot, "检测数据快照序列化失败，eventNo=" + message.getEventNo());
	}

	private String writeAlertDetail(List<DetectionAlertItem> alertItems) {
		return writeJson(alertItems, "检测预警详情序列化失败");
	}

	private String writeJson(Object value, String errorMessage) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID, errorMessage);
		}
	}

	private LabSensorDataConsumeResult buildResult(SensorDataMessage message, Lab lab, LabDetectionLog detectionLog,
			DetectionEvaluationResult evaluationResult, boolean duplicate) {
		return LabSensorDataConsumeResult.builder()
				.eventNo(detectionLog.getEventNo())
				.messageId(detectionLog.getMessageId())
				.lab(lab)
				.sensor(message.getSensor())
				.detectedAt(detectionLog.getDetectedAt())
				.status(detectionLog.getStatus())
				.alertItems(evaluationResult.getAlertItems())
				.unknownIndicatorNames(evaluationResult.getUnknownIndicatorNames())
				.detectionLog(detectionLog)
				.duplicate(duplicate)
				.build();
	}

	private LabSensorDataConsumeResult buildDuplicateResult(SensorDataMessage message, Lab lab,
			LabDetectionLog existingLog) {
		List<DetectionAlertItem> alertItems = readAlertItems(existingLog);
		return LabSensorDataConsumeResult.builder()
				.eventNo(existingLog.getEventNo())
				.messageId(existingLog.getMessageId())
				.lab(lab)
				.sensor(message.getSensor())
				.detectedAt(existingLog.getDetectedAt())
				.status(existingLog.getStatus())
				.alertItems(alertItems)
				.unknownIndicatorNames(List.of())
				.detectionLog(existingLog)
				.duplicate(true)
				.build();
	}

	private List<DetectionAlertItem> readAlertItems(LabDetectionLog detectionLog) {
		if (!StringUtils.hasText(detectionLog.getAlertDetail())) {
			return List.of();
		}
		try {
			return objectMapper.readValue(detectionLog.getAlertDetail(), ALERT_ITEM_LIST_TYPE);
		}
		catch (JsonProcessingException ex) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"检测日志预警详情反序列化失败，eventNo=" + detectionLog.getEventNo());
		}
	}
}
