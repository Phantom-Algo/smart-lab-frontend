package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入库记录表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_stock_in")
public class MaterialStockInDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 入库单号，全局唯一 */
    private String stockInNo;
    /** 关联批次ID */
    private Long batchId;
    /** 关联物资ID */
    private Long materialId;
    /** 入库数量 */
    private Integer quantity;
    /** 入库操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
