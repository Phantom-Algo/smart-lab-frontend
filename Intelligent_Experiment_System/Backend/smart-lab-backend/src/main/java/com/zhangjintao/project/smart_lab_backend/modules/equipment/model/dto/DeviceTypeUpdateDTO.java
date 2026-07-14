package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

/**
 * 设备类型更新DTO
 *
 * @author linbowei
 */
@Data
public class DeviceTypeUpdateDTO {
    /** 类型名称 */
    private String typeName;
    /** 类型描述 */
    private String description;
}
