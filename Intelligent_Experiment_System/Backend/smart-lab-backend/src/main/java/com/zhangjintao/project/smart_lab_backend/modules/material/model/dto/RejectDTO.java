package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回申请DTO — FR-0004 reject
 *
 * @author linbowei
 */
@Data
public class RejectDTO {
    /** 驳回原因 */
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 255)
    private String reason;
}
