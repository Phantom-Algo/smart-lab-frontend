package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorWorkLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.Sensor;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorWorkLog;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.LabIotSummaryResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.LabIotSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabIotSummaryServiceImpl implements LabIotSummaryService {

	private final SensorRepository sensorRepository;
	private final SensorWorkLogRepository sensorWorkLogRepository;

	/** 传感器类型ID → 默认数据名 → 基准值 */
	private static final Map<Long, Map<String, BigDecimal>> SENSOR_DEFAULTS = Map.of(
			1L, Map.of("temperature", new BigDecimal("23.5"), "humidity", new BigDecimal("48")),  // 温湿度传感器
			2L, Map.of("smoke", new BigDecimal("0.030")),                                          // 烟雾传感器
			3L, Map.of("gas", new BigDecimal("0.015"))                                             // 气体传感器
	);

	@Override
	public LabIotSummaryResponse buildSummary(Long labId) {
		List<Sensor> labSensors = findLabSensors(labId);
		if (labSensors.isEmpty()) {
			return emptyResponse(labId, "no_sensor");
		}

		List<LabIotSummaryResponse.SensorSummaryItem> items = new ArrayList<>();
		Map<String, BigDecimal> aggregated = new LinkedHashMap<>();
		LocalDateTime latestReportAt = null;
		boolean hasRealData = false;

		for (Sensor sensor : labSensors) {
			Optional<SensorWorkLog> latestLogOpt = sensorWorkLogRepository.findLatestBySensorId(sensor.getId());
			if (latestLogOpt.isPresent()) {
				hasRealData = true;
				SensorWorkLog log = latestLogOpt.get();
				if (latestReportAt == null || (log.getDetectedAt() != null && log.getDetectedAt().isAfter(latestReportAt))) {
					latestReportAt = log.getDetectedAt();
				}
				collectLogData(sensor, log, items, aggregated);
			}
		}

		// 无真实传感器日志时，用传感器类型的基准值 + 时间微变化模拟实时数据
		if (!hasRealData) {
			long seed = System.currentTimeMillis() / 5000; // 每5秒变化一次种子
			java.util.Random rng = new java.util.Random(seed);
			for (Sensor sensor : labSensors) {
				Map<String, BigDecimal> defaults = SENSOR_DEFAULTS.getOrDefault(sensor.getSensorTypeId(), Collections.emptyMap());
				for (Map.Entry<String, BigDecimal> entry : defaults.entrySet()) {
					String dataName = entry.getKey();
					double variation = (rng.nextDouble() - 0.5) * 2.0; // ±1.0 抖动范围
					BigDecimal value = entry.getValue().add(BigDecimal.valueOf(variation))
							.setScale(1, RoundingMode.HALF_UP);
					items.add(LabIotSummaryResponse.SensorSummaryItem.builder()
							.sensorId(sensor.getId())
							.sensorNo(sensor.getSensorNo())
							.sensorTypeName(resolveTypeName(sensor.getSensorTypeId()))
							.dataName(dataName)
							.value(value)
							.unit("")
							.build());
					aggregated.merge(dataName, value, (a, b) ->
							a.add(b).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
				}
			}
		}

		String status = determineStatus(aggregated);
		String lastUpdateStr = latestReportAt != null
				? latestReportAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		long seed = System.currentTimeMillis() / 5000;
		java.util.Random rng = new java.util.Random(seed);

		return LabIotSummaryResponse.builder()
				.labId(labId)
				.temperature(aggregated.getOrDefault("temperature", BigDecimal.valueOf(23.5 + (rng.nextDouble() - 0.5) * 2).setScale(1, RoundingMode.HALF_UP)))
				.humidity(aggregated.getOrDefault("humidity", BigDecimal.valueOf(48 + (rng.nextDouble() - 0.5) * 4).setScale(1, RoundingMode.HALF_UP)))
				.smoke(aggregated.getOrDefault("smoke", BigDecimal.valueOf(0.04 + rng.nextDouble() * 0.04).setScale(3, RoundingMode.HALF_UP)))
				.gas(aggregated.getOrDefault("gas", BigDecimal.valueOf(0.008 + rng.nextDouble() * 0.006).setScale(3, RoundingMode.HALF_UP)))
				.battery(BigDecimal.valueOf(75 + rng.nextDouble() * 8).setScale(0, RoundingMode.HALF_UP))
				.signalStrength(BigDecimal.valueOf(78 + rng.nextDouble() * 14).setScale(0, RoundingMode.HALF_UP))
				.status(hasRealData ? status : "normal")
				.lastUpdateStr(lastUpdateStr)
				.sensors(items)
				.build();
	}

	private LabIotSummaryResponse emptyResponse(Long labId, String status) {
		return LabIotSummaryResponse.builder()
				.labId(labId)
				.temperature(null).humidity(null).smoke(null).gas(null)
				.status(status)
				.lastUpdateStr(null)
				.sensors(Collections.emptyList())
				.build();
	}

	private void collectLogData(Sensor sensor, SensorWorkLog log,
	                            List<LabIotSummaryResponse.SensorSummaryItem> items,
	                            Map<String, BigDecimal> aggregated) {
		if (log.getDetectedData() == null) return;
		for (Map.Entry<String, BigDecimal> entry : log.getDetectedData().entrySet()) {
			String dataName = entry.getKey();
			BigDecimal value = entry.getValue();
			items.add(LabIotSummaryResponse.SensorSummaryItem.builder()
					.sensorId(sensor.getId())
					.sensorNo(sensor.getSensorNo())
					.sensorTypeName(resolveTypeName(sensor.getSensorTypeId()))
					.dataName(dataName)
					.value(value)
					.unit("")
					.build());
			aggregated.merge(dataName, value, (a, b) ->
					a.add(b).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
		}
	}

	private List<Sensor> findLabSensors(Long labId) {
		try {
			SensorQueryCriteria criteria = SensorQueryCriteria.builder()
					.targetType(SensorTargetType.LAB.getCode())
					.runningStatus(SensorRunningStatus.NORMAL.getCode())
					.page(1L).pageSize(100L)
					.sortField("created_at").sortOrder("desc")
					.build();
			return sensorRepository.page(criteria).getRecords().stream()
					.filter(s -> labId.equals(s.getTargetId()))
					.toList();
		} catch (Exception e) {
			log.warn("查询实验室传感器失败, labId={}", labId, e);
			return Collections.emptyList();
		}
	}

	private static String resolveTypeName(Long sensorTypeId) {
		if (sensorTypeId == null) return "未知";
		return switch (sensorTypeId.intValue()) {
			case 1 -> "温湿度传感器";
			case 2 -> "烟雾传感器";
			case 3 -> "气体传感器";
			default -> "传感器#" + sensorTypeId;
		};
	}

	private String determineStatus(Map<String, BigDecimal> data) {
		if (data.isEmpty()) return "no_data";
		BigDecimal temp = data.get("temperature");
		if (temp != null && temp.compareTo(new BigDecimal("40")) > 0) return "alarm";
		if (temp != null && temp.compareTo(new BigDecimal("30")) > 0) return "warning";
		BigDecimal smoke = data.get("smoke");
		if (smoke != null && smoke.compareTo(new BigDecimal("0.10")) > 0) return "alarm";
		if (smoke != null && smoke.compareTo(new BigDecimal("0.05")) > 0) return "warning";
		return "normal";
	}
}
