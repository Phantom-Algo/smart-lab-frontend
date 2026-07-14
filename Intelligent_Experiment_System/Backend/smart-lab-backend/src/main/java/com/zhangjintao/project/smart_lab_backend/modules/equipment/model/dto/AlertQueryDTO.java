package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

/**
 * 告警记录查询DTO
 */
@Data
public class AlertQueryDTO {
    private Long deviceId;
    private String paramName;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
