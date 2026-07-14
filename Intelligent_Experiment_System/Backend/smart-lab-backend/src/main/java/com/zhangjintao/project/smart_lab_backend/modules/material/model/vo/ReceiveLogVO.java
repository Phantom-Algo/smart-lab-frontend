package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用出库日志视图对象 — FR-0005
 *
 * @author linbowei
 */
@Data
@Builder
public class ReceiveLogVO {
    private Long id;
    /** 关联申请单ID */
    private Long applyId;
    /** 申请单号 */
    private String applyNo;
    /** 物资ID */
    private Long materialId;
    /** 物资名称 */
    private String materialName;
    /** 本次领用总数量 */
    private Integer totalQuantity;
    /** 领用人ID */
    private Long receiverId;
    /** 出库操作人ID */
    private Long outOperatorId;
    /** 出库时间 */
    private LocalDateTime outTime;
    /** 备注 */
    private String remark;
    /** 批次扣减明细 */
    private List<BatchDeductDetail> batchDetails;
    /** 创建时间 */
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class BatchDeductDetail {
        private Long batchId;
        private String batchNo;
        private Integer deductedQuantity;
    }
}
