package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 传感器数据上报DTO（物联网环境 → 设备子系统）
 * <p>
 * 设备子系统定义此契约，物联网环境子系统按此格式上报。
 * </p>
 *
 * @author linbowei
 */
@Data
public class SensorReportDTO {
    /** 事件编号（幂等标识，物联网侧生成） */
    @NotNull(message = "事件编号不能为空")
    private String eventNo;
    /** 传感器ID */
    @NotNull(message = "传感器ID不能为空")
    private Long sensorId;
    /** 目标设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    /** 监测数据：key=指标名称，value=指标值 */
    @NotEmpty(message = "监测数据不能为空")
    private Map<String, SensorMetricValueDTO> metrics;
}
