package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class DeviceVO {
    private Long id;
    private String deviceNo;
    private String name;
    private Long deviceTypeId;
    private String deviceTypeName;
    private Long labId;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private LocalDate expireDate;
    private Integer deviceStatus;
    private String deviceStatusDesc;
    private Integer healthScore;
    private Integer version;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
