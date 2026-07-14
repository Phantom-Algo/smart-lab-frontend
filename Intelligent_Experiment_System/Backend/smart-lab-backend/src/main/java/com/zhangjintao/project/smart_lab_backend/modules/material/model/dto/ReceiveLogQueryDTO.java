package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领用记录查询DTO — FR-0005
 *
 * @author linbowei
 */
@Data
public class ReceiveLogQueryDTO {
    /** 物资ID */
    private Long materialId;
    /** 领用人ID */
    private Long receiverId;
    /** 申请状态：1待审核 2审批通过 3驳回 4已取消 */
    private Integer applyStatus;
    /** 出库开始时间 */
    private LocalDateTime startTime;
    /** 出库结束时间 */
    private LocalDateTime endTime;
    /** 页码 */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 20;
}
