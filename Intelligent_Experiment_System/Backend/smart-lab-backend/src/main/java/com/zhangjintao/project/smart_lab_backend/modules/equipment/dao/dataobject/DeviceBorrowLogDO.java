package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备借用日志表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_borrow_log")
public class DeviceBorrowLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Long deviceId;
    private Long borrowerId;
    private Long outOperatorId;
    private Long returnOperatorId;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime actualOutTime;
    private LocalDateTime actualReturnTime;
    private Integer status;
    private Integer isOverdue;
    private String returnRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
