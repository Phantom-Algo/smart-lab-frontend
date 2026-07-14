package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备创建DTO
 *
 * @author linbowei
 */
@Data
public class DeviceCreateDTO {
    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    private String name;
    /** 设备类型ID */
    @NotNull(message = "设备类型不能为空")
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
