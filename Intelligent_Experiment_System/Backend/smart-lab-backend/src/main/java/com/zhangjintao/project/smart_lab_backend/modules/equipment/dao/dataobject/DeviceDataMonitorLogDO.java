package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备监测日志表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_data_monitor_log")
public class DeviceDataMonitorLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long sensorId;
    private String detectedData;
    private Integer isSafe;
    private LocalDateTime detectedAt;
    private LocalDateTime createdAt;
}
