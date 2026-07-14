package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FaultStatsVO {
    private long totalDevices;
    private long faultDevices;
    private long totalRepairs;
    private BigDecimal faultRate;
}
