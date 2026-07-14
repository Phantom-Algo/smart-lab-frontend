package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借用申请视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class BorrowApplyVO {
    private Long id;
    private String applyNo;
    private Long deviceId;
    private String deviceNo;
    private String deviceName;
    private Long applicantId;
    private String applicantName;
    private Integer applicantType;
    private Long approverId;
    private LocalDateTime expectedStartTime;
    private LocalDateTime expectedReturnTime;
    private String purpose;
    private Integer applyStatus;
    private String applyStatusDesc;
    private String rejectReason;
    private Integer isOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
