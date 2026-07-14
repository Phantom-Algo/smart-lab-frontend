package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监测指标视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class MetricIndicatorVO {
    private Long id;
    private Long equipmentTypeId;
    private String metricName;
    private String unit;
    private String safeScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
