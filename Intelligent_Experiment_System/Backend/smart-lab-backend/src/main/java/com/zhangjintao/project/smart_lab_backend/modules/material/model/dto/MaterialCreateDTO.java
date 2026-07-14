package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 物资创建DTO
 *
 * @author linbowei
 */
@Data
public class MaterialCreateDTO {
    /** 物资名称 */
    @NotBlank(message = "物资名称不能为空")
    private String materialName;
    /** 所属实验室ID */
    @NotNull(message = "所属实验室不能为空")
    private Long labId;
    /** 计量单位 */
    @NotBlank(message = "计量单位不能为空")
    private String unit;
    /** 安全库存量 */
    @Min(value = 0, message = "安全库存不能为负")
    private Integer safetyStock = 0;
}
