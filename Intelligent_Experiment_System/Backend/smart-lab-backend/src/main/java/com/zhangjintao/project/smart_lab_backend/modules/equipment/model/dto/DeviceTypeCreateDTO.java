package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备类型创建DTO
 *
 * @author linbowei
 */
@Data
public class DeviceTypeCreateDTO {
    /** 类型名称 */
    @NotBlank(message = "类型名称不能为空")
    private String typeName;
    /** 类型描述 */
    private String description;
}
