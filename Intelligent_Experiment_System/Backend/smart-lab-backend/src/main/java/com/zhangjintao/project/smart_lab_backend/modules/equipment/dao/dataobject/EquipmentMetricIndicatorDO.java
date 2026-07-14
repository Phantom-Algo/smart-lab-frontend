package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备监测指标表 DO
 *
 * @author linbowei
 */
@Data
@TableName("equipment_metric_indicator")
public class EquipmentMetricIndicatorDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long equipmentTypeId;
    private String metricName;
    private String unit;
    private String safeScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
