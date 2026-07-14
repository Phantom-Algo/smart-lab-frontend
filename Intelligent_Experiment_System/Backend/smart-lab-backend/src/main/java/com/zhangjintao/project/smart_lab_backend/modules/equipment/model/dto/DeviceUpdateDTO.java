package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备更新DTO
 *
 * @author linbowei
 */
@Data
public class DeviceUpdateDTO {
    /** 设备名称 */
    private String name;
    /** 设备类型ID */
    private Long deviceTypeId;
    /** 所属实验室ID */
    private Long labId;
    /** 购入日期 */
    private LocalDate purchaseDate;
    /** 购置价格 */
    private BigDecimal purchasePrice;
    /** 保修截止日期 */
    private LocalDate expireDate;
    /** 备注 */
    private String remark;
}
