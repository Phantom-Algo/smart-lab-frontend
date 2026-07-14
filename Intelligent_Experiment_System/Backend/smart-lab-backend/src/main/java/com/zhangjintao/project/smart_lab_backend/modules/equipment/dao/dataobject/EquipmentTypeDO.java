package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备类型表 DO
 *
 * @author linbowei
 */
@Data
@TableName("equipment_type")
public class EquipmentTypeDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
