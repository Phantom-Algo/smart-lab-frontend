package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备类型视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class DeviceTypeVO {
    private Long id;
    private String typeName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
