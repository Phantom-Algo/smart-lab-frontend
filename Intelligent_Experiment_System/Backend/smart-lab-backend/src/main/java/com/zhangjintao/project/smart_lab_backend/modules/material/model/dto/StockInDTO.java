package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 入库DTO — FR-0002 stockIn
 *
 * @author linbowei
 */
@Data
public class StockInDTO {
    /** 物资ID */
    @NotNull(message = "物资ID不能为空")
    private Long materialId;
    /** 入库数量 */
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    private Integer quantity;
    /** 有效期至 */
    @NotNull(message = "有效期不能为空")
    private LocalDate expiryDate;
    /** 备注 */
    private String remark;
}
