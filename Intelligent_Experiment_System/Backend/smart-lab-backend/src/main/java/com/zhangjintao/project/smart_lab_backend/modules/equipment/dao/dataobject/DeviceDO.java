package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备主表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device")
public class DeviceDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceNo;
    private String name;
    private Long deviceTypeId;
    private Long labId;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private LocalDate expireDate;
    private Integer deviceStatus;
    private Integer healthScore;
    private Integer version;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
