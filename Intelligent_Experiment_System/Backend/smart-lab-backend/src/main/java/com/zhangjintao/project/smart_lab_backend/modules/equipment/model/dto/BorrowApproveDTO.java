package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 借用审批DTO
 *
 * @author linbowei
 */
@Data
public class BorrowApproveDTO {
    /** 审批人ID（由Controller从SecurityContext注入） */
    private Long approverId;
    /** 是否通过 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
    /** 驳回原因（驳回时必填） */
    private String rejectReason;
}
