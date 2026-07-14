package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 物联网环境子系统 - RocketMQ 配置属性类
 * <p>
 * 绑定 application-dev.yml 中 smart-lab.iot.mq 前缀下的配置，
 * 统一管理 Topic、Tag、Producer Group、Consumer Group 等 MQ 相关命名。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "smart-lab.iot.mq")
public class IotMqProperties {

    /** RocketMQ Topic 名称，默认 iot-sensor-data */
    private String topic = "iot-sensor-data";

    /** 生产者组名称 */
    private String producerGroup = "iot-sensor-data-producer";

    /** Tag 名称集合 */
    private Tags tags = new Tags();

    /** Consumer Group 名称集合 */
    private ConsumerGroups consumerGroups = new ConsumerGroups();

    /**
     * RocketMQ Tag 名称
     */
    @Data
    public static class Tags {
        /** 推送至实验场地管理子系统的 Tag */
        private String labSensorData = "lab_sensor_data";

        /** 推送至智能化设备管理子系统的 Tag */
        private String deviceSensorData = "device_sensor_data";
    }

    /**
     * RocketMQ Consumer Group 名称
     */
    @Data
    public static class ConsumerGroups {
        /** 实验场地管理子系统的消费者组 */
        private String labSensorDataConsumer = "lab-sensor-data-consumer";

        /** 智能化设备管理子系统的消费者组 */
        private String deviceSensorDataConsumer = "device-sensor-data-consumer";
    }
}
