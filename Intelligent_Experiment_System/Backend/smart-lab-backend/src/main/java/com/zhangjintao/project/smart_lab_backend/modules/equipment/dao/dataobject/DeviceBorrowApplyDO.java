package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备借用申请表 DO
 *
 * @author linbowei
 */
@Data
@TableName("device_borrow_apply")
public class DeviceBorrowApplyDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long deviceId;
    private Long applicantId;
    private Long approverId;
    private LocalDateTime expectedStartTime;
    private LocalDateTime expectedReturnTime;
    private String purpose;
    private Integer applyStatus;
    private String rejectReason;
    private Integer isOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
