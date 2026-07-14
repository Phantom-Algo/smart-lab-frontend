package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 入库结果视图对象 — TDD StockInResultVO
 *
 * @author linbowei
 */
@Data
@Builder
public class StockInResultVO {
    /** 生成的批次编号 */
    private String batchNo;
    /** 入库单号 */
    private String stockInNo;
    /** 入库数量 */
    private Integer quantity;
    /** 入库后当前库存 */
    private Integer newCurrentStock;
}
