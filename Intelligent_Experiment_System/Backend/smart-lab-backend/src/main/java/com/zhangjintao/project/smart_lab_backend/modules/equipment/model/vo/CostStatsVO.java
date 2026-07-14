package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CostStatsVO {
    private long totalRepairs;
    private BigDecimal totalCost;
    private BigDecimal avgCost;
    private BigDecimal maxCost;
}
