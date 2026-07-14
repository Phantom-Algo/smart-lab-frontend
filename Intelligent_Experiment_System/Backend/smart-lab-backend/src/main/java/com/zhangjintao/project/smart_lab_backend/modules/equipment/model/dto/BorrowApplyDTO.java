package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借用申请DTO
 *
 * @author linbowei
 */
@Data
public class BorrowApplyDTO {
    /** 申请人ID（由Controller从SecurityContext注入） */
    private Long applicantId;
    /** 预计开始使用时间 */
    @NotNull(message = "预计开始时间不能为空")
    private LocalDateTime expectedStartTime;
    /** 预计归还时间 */
    @NotNull(message = "预计归还时间不能为空")
    private LocalDateTime expectedReturnTime;
    /** 借用用途 */
    @NotBlank(message = "借用用途不能为空")
    private String purpose;
}
