package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物资详情视图对象（含批次列表） — TDD MaterialDetailVO
 *
 * @author linbowei
 */
@Data
@Builder
public class MaterialDetailVO {
    private Long id;
    private String materialNo;
    private String materialName;
    private Long labId;
    private String unit;
    private Integer safetyStock;
    private Integer currentStock;
    /** 该物资下所有批次 */
    private List<MaterialBatchVO> batches;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
