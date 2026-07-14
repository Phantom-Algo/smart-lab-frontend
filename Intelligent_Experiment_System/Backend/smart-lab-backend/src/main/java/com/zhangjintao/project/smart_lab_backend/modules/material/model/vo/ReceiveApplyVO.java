package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领用申请视图对象 — FR-0003/FR-0005
 *
 * @author linbowei
 */
@Data
@Builder
public class ReceiveApplyVO {
    private Long id;
    /** 申请单号 */
    private String applyNo;
    /** 物资ID */
    private Long materialId;
    /** 物资名称 */
    private String materialName;
    /** 申请人ID */
    private Long applicantId;
    /** 审批人ID */
    private Long approverId;
    /** 申请领用数量 */
    private Integer expectedQuantity;
    /** 用途 */
    private String purpose;
    /** 申请状态 */
    private Integer applyStatus;
    /** 申请状态描述 */
    private String applyStatusDesc;
    /** 驳回原因 */
    private String rejectReason;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
