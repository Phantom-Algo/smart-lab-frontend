package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备维修记录表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_repair_record")
public class DeviceRepairRecordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String repairNo;
    private Long deviceId;
    private Long reporterId;
    private Long handlerId;
    private LocalDateTime reportTime;
    private String faultDesc;
    private Integer repairStatus;
    private String repairDesc;
    private LocalDateTime completedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
