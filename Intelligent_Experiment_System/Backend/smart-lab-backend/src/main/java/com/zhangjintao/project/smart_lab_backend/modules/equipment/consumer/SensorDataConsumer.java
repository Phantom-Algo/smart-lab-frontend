package com.zhangjintao.project.smart_lab_backend.modules.equipment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.SensorMetricValueDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.SensorReportDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备传感器数据 RocketMQ 消费者
 * <p>
 * 消费物联网环境子系统（sensor_env）通过 RocketMQ 推送的设备传感器数据。
 * Topic: iot-sensor-data, Tag: device_sensor_data
 * </p>
 *
 * @author linbowei
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${smart-lab.iot.mq.topic:iot-sensor-data}",
        selectorType = SelectorType.TAG,
        selectorExpression = "${smart-lab.iot.mq.tags.device-sensor-data:device_sensor_data}",
        consumerGroup = "${smart-lab.iot.mq.consumer-groups.device-sensor-data-consumer:device-sensor-data-consumer}"
)
public class SensorDataConsumer implements RocketMQListener<MessageExt> {

    /** 目标类型：设备（与 sensor_env 的 TargetInfo.targetType=1 对应） */
    private static final Integer TARGET_TYPE_DEVICE = 1;

    private final DeviceSensorService deviceSensorService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = messageExt.getMsgId();
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("收到设备传感器MQ消息，msgId={}, body={}", msgId, body);

        try {
            // 1. 反序列化 SensorDataMessage
            SensorDataMessage sensorMsg = objectMapper.readValue(body, SensorDataMessage.class);

            // 2. 仅处理 targetType=1（设备）的消息；targetType=2 是实验室数据，由 lab_management 消费
            if (sensorMsg.getTarget() == null || !TARGET_TYPE_DEVICE.equals(sensorMsg.getTarget().getTargetType())) {
                log.debug("忽略非设备类型消息，targetType={}, msgId={}",
                        sensorMsg.getTarget() != null ? sensorMsg.getTarget().getTargetType() : null, msgId);
                return;
            }

            // 3. 转换为 SensorReportDTO
            SensorReportDTO report = convertToReportDTO(sensorMsg);

            // 4. 调用设备传感器服务处理
            deviceSensorService.receiveSensorData(report);
            log.info("设备传感器数据处理成功，eventNo={}, deviceId={}", report.getEventNo(), report.getDeviceId());

        } catch (Exception e) {
            log.error("设备传感器MQ消息处理失败，msgId={}, body={}", msgId, body, e);
            // 不抛异常，避免无限重试；失败的告警由监控补全
        }
    }

    /**
     * SensorDataMessage → SensorReportDTO 转换
     * <p>
     * 字段映射：
     * <ul>
     *   <li>eventNo → eventNo（幂等标识）</li>
     *   <li>sensor.sensorId → sensorId</li>
     *   <li>target.targetId → deviceId（目标设备ID）</li>
     *   <li>detectedData[dataName].value → metrics[dataName].quantity</li>
     *   <li>detectedData[dataName].unit → metrics[dataName].unit</li>
     * </ul>
     * </p>
     */
    private SensorReportDTO convertToReportDTO(SensorDataMessage sensorMsg) {
        SensorReportDTO report = new SensorReportDTO();
        report.setEventNo(sensorMsg.getEventNo());
        report.setSensorId(sensorMsg.getSensor() != null ? sensorMsg.getSensor().getSensorId() : null);
        report.setDeviceId(sensorMsg.getTarget().getTargetId());

        // 转换 detectedData Map<String, DataItem> → Map<String, SensorMetricValueDTO>
        Map<String, SensorMetricValueDTO> metrics = new HashMap<>();
        if (sensorMsg.getDetectedData() != null) {
            sensorMsg.getDetectedData().forEach((dataName, dataItem) -> {
                SensorMetricValueDTO valueDTO = new SensorMetricValueDTO();
                valueDTO.setQuantity(dataItem.getValue() != null ? dataItem.getValue() : BigDecimal.ZERO);
                valueDTO.setUnit(dataItem.getUnit());
                metrics.put(dataName, valueDTO);
            });
        }
        report.setMetrics(metrics);
        return report;
    }
}
