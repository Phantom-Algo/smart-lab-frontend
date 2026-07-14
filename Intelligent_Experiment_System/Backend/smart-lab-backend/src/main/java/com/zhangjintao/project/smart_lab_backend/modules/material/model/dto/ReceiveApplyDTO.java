package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 领用申请DTO — FR-0003
 *
 * @author linbowei
 */
@Data
public class ReceiveApplyDTO {
    /** 物资ID */
    @NotNull(message = "物资ID不能为空")
    private Long materialId;
    /** 申请领用数量 */
    @NotNull(message = "领用数量不能为空")
    @Min(value = 1, message = "领用数量必须大于0")
    private Integer expectedQuantity;
    /** 用途（可选） */
    @Size(max = 255)
    private String purpose;
}
