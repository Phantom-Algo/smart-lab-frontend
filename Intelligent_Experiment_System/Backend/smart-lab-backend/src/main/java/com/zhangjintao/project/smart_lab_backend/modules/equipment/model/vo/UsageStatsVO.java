package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class UsageStatsVO {
    private long totalDevices;
    private long borrowedDevices;
    private long totalBorrowDays;
    private BigDecimal usageRate;
}
