package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借用日志视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class BorrowLogVO {
    private Long id;
    private Long applyId;
    private Long deviceId;
    private String deviceName;
    private Long borrowerId;
    private Long outOperatorId;
    private Long returnOperatorId;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime actualOutTime;
    private LocalDateTime actualReturnTime;
    private Integer status;
    private String statusDesc;
    private Integer isOverdue;
    private String returnRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
