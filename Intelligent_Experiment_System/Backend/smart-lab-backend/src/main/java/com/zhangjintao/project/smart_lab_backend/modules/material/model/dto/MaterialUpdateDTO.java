package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import lombok.Data;

/**
 * 物资更新DTO
 *
 * @author linbowei
 */
@Data
public class MaterialUpdateDTO {
    /** 物资名称 */
    private String materialName;
    /** 所属实验室ID */
    private Long labId;
    /** 计量单位 */
    private String unit;
    /** 安全库存量 */
    private Integer safetyStock;
}
