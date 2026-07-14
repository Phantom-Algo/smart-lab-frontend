package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备告警记录表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_alert_record")
public class DeviceAlertRecordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long sensorId;
    private String paramName;
    private BigDecimal actualValue;
    private String safeScope;
    private Integer exceedDirection;
    private LocalDateTime alertTime;
    private Integer status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime createdAt;
}
