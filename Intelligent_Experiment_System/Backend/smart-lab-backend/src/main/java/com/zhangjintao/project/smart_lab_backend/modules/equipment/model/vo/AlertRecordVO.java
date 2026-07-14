package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AlertRecordVO {
    private Long id;
    private Long deviceId;
    private String deviceName;
    private Long sensorId;
    private String paramName;
    private BigDecimal actualValue;
    private String safeScope;
    private Integer exceedDirection;
    private LocalDateTime alertTime;
    private Integer status;
    private String statusDesc;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime createdAt;
}
