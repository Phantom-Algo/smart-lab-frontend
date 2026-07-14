package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceHealthVO {
    private Long deviceId;
    private String deviceName;
    private Integer healthScore;
    private String grade;
    private Integer yearFactor;
    private Integer warrantyFactor;
    private Integer borrowFactor;
    private Integer repairFactor;
    private Integer costFactor;
    private Integer alertFactor;
    private boolean scrapAdvisory;
}
