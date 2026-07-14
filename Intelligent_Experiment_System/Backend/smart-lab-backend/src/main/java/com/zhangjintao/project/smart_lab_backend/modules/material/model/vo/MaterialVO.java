package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物资视图对象
 *
 * @author linbowei
 */
@Data
@Builder
public class MaterialVO {
    private Long id;
    /** 物资编号 */
    private String materialNo;
    /** 物资名称 */
    private String materialName;
    /** 所属实验室ID */
    private Long labId;
    /** 计量单位 */
    private String unit;
    /** 安全库存量 */
    private Integer safetyStock;
    /** 当前在库总量 */
    private Integer currentStock;
    /** 批次数量 */
    private Integer batchCount;
    /** 最早过期批次的过期日期 */
    private LocalDate earliestExpiryDate;
    /** 最早过期批次编号 */
    private String earliestBatchNo;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
