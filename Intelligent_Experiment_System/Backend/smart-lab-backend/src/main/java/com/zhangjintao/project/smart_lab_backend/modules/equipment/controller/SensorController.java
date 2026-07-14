package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

/**
 * 传感器数据HTTP入口已废弃。
 * <p>
 * 现在通过 {@code SensorDataConsumer} 消费 RocketMQ 消息接收物联网传感器数据。
 * Topic: iot-sensor-data, Tag: device_sensor_data
 * </p>
 *
 * @deprecated 替代为 {@link com.zhangjintao.project.smart_lab_backend.modules.equipment.consumer.SensorDataConsumer}
 */
@Deprecated
public class SensorController {
    // 保持空类以兼容编译，功能已迁移至 SensorDataConsumer
}
