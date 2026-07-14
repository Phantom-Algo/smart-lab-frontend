package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 传感器数据消息体 - 用于在 RocketMQ 中传输的标准化数据结构
 * <p>
 * 本消息体由物联网环境子系统（IoT）生产，通过 RocketMQ 分发至下游子系统消费。
 * 下游子系统根据 {@code target.targetType} 决定是否处理该消息：
 * <ul>
 *   <li>targetType=1（设备）→ Tag: {@code device_sensor_data}，由智能化设备管理子系统消费</li>
 *   <li>targetType=2（实验室）→ Tag: {@code lab_sensor_data}，由实验场地管理子系统消费</li>
 * </ul>
 * </p>
 *
 * <h3>消息流转示意</h3>
 * <pre>
 * 模拟程序 → POST /api/iot/sensor-data → 校验&持久化
 *     → RocketMQ Topic: iot-sensor-data
 *         ├── Tag: lab_sensor_data    → 实验场地管理子系统（lab_detection_log）
 *         └── Tag: device_sensor_data → 智能化设备管理子系统（device_status_snapshot）
 * </pre>
 *
 * @author 张津涛
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataMessage {

    /** 消息全局唯一标识（UUID v4），用于日志追踪 */
    @JsonProperty("messageId")
    private String messageId;

    /** 事件编号，格式: iot.sensor.data.report:{16位随机数}，消费方用于幂等去重 */
    @JsonProperty("eventNo")
    private String eventNo;

    /** 事件发生时间（ISO 8601 格式） */
    @JsonProperty("occurredAt")
    private LocalDateTime occurredAt;

    /** 来源系统标识，固定值 "iot" */
    @JsonProperty("sourceSystem")
    private String sourceSystem;

    /** 传感器信息 */
    @JsonProperty("sensor")
    private SensorInfo sensor;

    /** 绑定目标信息 */
    @JsonProperty("target")
    private TargetInfo target;

    /** 检测数据，key 为数据名称（如 temperature/humidity），value 为检测值及单位 */
    @JsonProperty("detectedData")
    private Map<String, DataItem> detectedData;

    /** 传感器采集时间（ISO 8601 格式） */
    @JsonProperty("detectedAt")
    private LocalDateTime detectedAt;

    /**
     * 传感器基本信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorInfo {
        /** 传感器主键 ID */
        @JsonProperty("sensorId")
        private Long sensorId;

        /** 传感器编号，全局唯一 */
        @JsonProperty("sensorNo")
        private String sensorNo;

        /** 传感器类型 ID */
        @JsonProperty("sensorTypeId")
        private Long sensorTypeId;

        /** 传感器类型名称（冗余字段，便于消费方展示） */
        @JsonProperty("sensorTypeName")
        private String sensorTypeName;
    }

    /**
     * 绑定目标信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetInfo {
        /** 绑定目标类型：1=设备, 2=实验室 */
        @JsonProperty("targetType")
        private Integer targetType;

        /** 绑定目标 ID */
        @JsonProperty("targetId")
        private Long targetId;
    }

    /**
     * 单个检测数据项（数值 + 单位）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataItem {
        /** 检测数值 */
        @JsonProperty("value")
        private BigDecimal value;

        /** 检测数据单位，如 °C、%RH、ppm */
        @JsonProperty("unit")
        private String unit;
    }
}
