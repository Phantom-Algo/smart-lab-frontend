package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 监测指标配置DTO
 *
 * @author linbowei
 */
@Data
public class MetricIndicatorConfigDTO {
    /** 设备类型ID */
    @NotNull(message = "设备类型ID不能为空")
    private Long equipmentTypeId;
    /** 指标名称（同一类型下唯一） */
    @NotBlank(message = "指标名称不能为空")
    private String metricName;
    /** 指标单位 */
    private String unit;
    /** 安全范围（JSON格式，如{"min":30,"max":70}） */
    private String safeScope;
}
