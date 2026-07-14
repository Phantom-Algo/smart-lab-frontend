package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物资批次视图对象 — FR-0006
 *
 * @author linbowei
 */
@Data
@Builder
public class MaterialBatchVO {
    private Long id;
    /** 批次编号 */
    private String batchNo;
    /** 物资ID */
    private Long materialId;
    /** 物资名称 */
    private String materialName;
    /** 入库数量 */
    private Integer quantity;
    /** 在库余量 */
    private Integer remaining;
    /** 有效期至 */
    private LocalDate expiryDate;
    /** 入库时间 */
    private LocalDateTime stockInTime;
    /** 批次状态 */
    private Integer batchStatus;
    /** 批次状态描述 */
    private String batchStatusDesc;
    /** 入库操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
