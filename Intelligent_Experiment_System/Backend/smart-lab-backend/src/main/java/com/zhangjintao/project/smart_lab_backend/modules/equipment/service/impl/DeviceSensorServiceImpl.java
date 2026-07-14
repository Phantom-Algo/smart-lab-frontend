package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDataMonitorLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentMetricIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceAlertRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceDataMonitorLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentMetricIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.AlertStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.SensorMetricValueDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.SensorReportDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 传感器数据接收服务实现
 * <p>
 * 一次IoT推送 = 一条监测日志（含全部指标快照） + 零到多条告警记录（超限指标逐条记录）。
 * </p>
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSensorServiceImpl implements DeviceSensorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** 超限方向：超上限 */
    private static final int EXCEED_UPPER = 1;
    /** 超限方向：低下限 */
    private static final int EXCEED_LOWER = 2;

    private final DeviceRepository deviceRepository;
    private final EquipmentMetricIndicatorRepository metricIndicatorRepository;
    private final DeviceDataMonitorLogRepository monitorLogRepository;
    private final DeviceAlertRecordRepository alertRepository;
    private final MessagePublisher messagePublisher;

    @Override
    @Transactional
    public void receiveSensorData(SensorReportDTO report) {
        // 1. 查找设备
        DeviceDO device = deviceRepository.findById(report.getDeviceId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));

        // 2. 获取该设备类型的所有监测指标（用于比对）
        Long typeId = device.getDeviceTypeId();
        List<EquipmentMetricIndicatorDO> indicators = metricIndicatorRepository.findByEquipmentTypeId(typeId);
        if (indicators.isEmpty()) {
            log.debug("设备类型 typeId={} 没有配置监测指标，跳过安全校验", typeId);
            return;
        }
        // 转为 Map 方便查找
        Map<String, EquipmentMetricIndicatorDO> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(EquipmentMetricIndicatorDO::getMetricName, i -> i));

        // 3. 遍历指标，安全范围比对
        List<DeviceAlertRecordDO> alerts = new ArrayList<>();
        // 构建完整监测数据快照（含全部指标）
        Map<String, Object> snapshot = new LinkedHashMap<>();
        boolean allSafe = true;

        for (Map.Entry<String, SensorMetricValueDTO> entry : report.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            SensorMetricValueDTO value = entry.getValue();
            if (value == null || value.getQuantity() == null) continue;

            // 填充快照
            Map<String, Object> metricSnapshot = new LinkedHashMap<>();
            metricSnapshot.put("quantity", value.getQuantity());
            if (value.getUnit() != null) metricSnapshot.put("unit", value.getUnit());
            snapshot.put(metricName, metricSnapshot);

            // 查找该指标配置
            EquipmentMetricIndicatorDO indicator = indicatorMap.get(metricName);
            if (indicator == null || indicator.getSafeScope() == null) {
                log.debug("指标 {} 未配置或无安全范围，跳过比对", metricName);
                continue;
            }

            // 比对安全范围
            CheckResult result = checkAgainstSafeScope(value.getQuantity(), indicator.getSafeScope());
            if (!result.isSafe()) {
                allSafe = false;
                alerts.add(buildAlertRecord(device, report.getSensorId(),
                        metricName, value, indicator.getSafeScope(), result.getExceedDirection()));
            }
        }

        // 4. 写入一条监测日志（内嵌全部指标快照 + 综合安全标记）
        DeviceDataMonitorLogDO logDO = new DeviceDataMonitorLogDO();
        logDO.setDeviceId(device.getId());
        logDO.setSensorId(report.getSensorId());
        logDO.setDetectedData(toJsonString(snapshot));
        logDO.setIsSafe(allSafe ? 1 : 0);
        logDO.setDetectedAt(LocalDateTime.now());
        logDO.setCreatedAt(LocalDateTime.now());
        monitorLogRepository.batchInsert(java.util.List.of(logDO));

        // 5. 告警写入 + 推送
        if (!alerts.isEmpty()) {
            alertRepository.batchInsert(alerts);
            notifySensorAlert(device, alerts);
        }
    }

    // ========== 安全范围比对 ==========

    /**
     * 检查实际值是否在安全范围内，返回结果（安全状态 + 超限方向）。
     */
    private CheckResult checkAgainstSafeScope(BigDecimal actualValue, String safeScopeJson) {
        try {
            SafeRange scope = OBJECT_MAPPER.readValue(safeScopeJson, SafeRange.class);
            if (scope.getMax() != null && actualValue.compareTo(scope.getMax()) > 0) {
                return CheckResult.unsafe(EXCEED_UPPER);
            }
            if (scope.getMin() != null && actualValue.compareTo(scope.getMin()) < 0) {
                return CheckResult.unsafe(EXCEED_LOWER);
            }
            return CheckResult.safe();
        } catch (Exception e) {
            log.error("安全范围解析失败，safeScope={}", safeScopeJson, e);
            return CheckResult.safe();
        }
    }

    @Data
    private static class SafeRange {
        private BigDecimal min;
        private BigDecimal max;
    }

    private static class CheckResult {
        private final boolean safe;
        private final int exceedDirection;

        private CheckResult(boolean safe, int exceedDirection) {
            this.safe = safe;
            this.exceedDirection = exceedDirection;
        }

        static CheckResult safe() {
            return new CheckResult(true, 0);
        }

        static CheckResult unsafe(int direction) {
            return new CheckResult(false, direction);
        }

        boolean isSafe() { return safe; }
        int getExceedDirection() { return exceedDirection; }
    }

    // ========== 告警记录构建 ==========

    private DeviceAlertRecordDO buildAlertRecord(DeviceDO device, Long sensorId, String paramName,
                                                  SensorMetricValueDTO value, String safeScope, int exceedDirection) {
        DeviceAlertRecordDO alert = new DeviceAlertRecordDO();
        alert.setDeviceId(device.getId());
        alert.setSensorId(sensorId);
        alert.setParamName(paramName);
        alert.setActualValue(value.getQuantity());
        alert.setSafeScope(safeScope);
        alert.setExceedDirection(exceedDirection);
        alert.setAlertTime(LocalDateTime.now());
        alert.setStatus(AlertStatus.UNHANDLED.getCode());
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }

    // ========== 消息通知 ==========

    private void notifySensorAlert(DeviceDO device, List<DeviceAlertRecordDO> alerts) {
        try {
            MessagePublishCommand command = MessagePublishCommand.builder()
                    .eventNo(MessageEventNoGenerator.generate("equipment.warning.sensor.alert"))
                    .sourceSystem("equipment")
                    .messageType("equipment.warning.sensor.alert")
                    .targetType(TargetType.ALL_MANAGER)
                    .templateCode("equipment.warning.sensor.alert")
                    .params(Map.of("deviceName", device.getName(), "alertCount", String.valueOf(alerts.size())))
                    .occurredAt(LocalDateTime.now())
                    .build();
            messagePublisher.publish(command);
        } catch (Exception e) {
            log.error("传感器告警消息通知发送失败", e);
        }
    }

    // ========== 工具方法 ==========

    private String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }
}
