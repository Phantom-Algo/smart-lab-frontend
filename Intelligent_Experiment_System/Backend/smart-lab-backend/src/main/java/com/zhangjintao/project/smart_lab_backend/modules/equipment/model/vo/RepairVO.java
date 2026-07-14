package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 维修记录视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class RepairVO {
    private Long id;
    private String repairNo;
    private Long deviceId;
    private String deviceName;
    private Long reporterId;
    private Long handlerId;
    private LocalDateTime reportTime;
    private String faultDesc;
    private Integer repairStatus;
    private String repairStatusDesc;
    private String repairDesc;
    private LocalDateTime completedAt;
    private LocalDateTime acceptedAt;
    private List<RepairCostItem> costs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class RepairCostItem {
        private Long id;
        private BigDecimal cost;
        private String remark;
    }
}
