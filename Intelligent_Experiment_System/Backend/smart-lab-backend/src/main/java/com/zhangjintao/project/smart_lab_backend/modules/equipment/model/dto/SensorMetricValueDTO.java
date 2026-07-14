package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 传感器指标值DTO
 *
 * @author linbowei
 */
@Data
public class SensorMetricValueDTO {
    /** 指标数值 */
    @NotNull(message = "指标数值不能为空")
    private BigDecimal quantity;
    /** 指标单位 */
    private String unit;
}
