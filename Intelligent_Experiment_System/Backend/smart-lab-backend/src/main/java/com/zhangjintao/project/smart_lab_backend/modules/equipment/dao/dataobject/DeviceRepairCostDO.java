package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备维修费用明细表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_repair_cost")
public class DeviceRepairCostDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repairId;
    private BigDecimal cost;
    private String remark;
    private LocalDateTime createdAt;
}
