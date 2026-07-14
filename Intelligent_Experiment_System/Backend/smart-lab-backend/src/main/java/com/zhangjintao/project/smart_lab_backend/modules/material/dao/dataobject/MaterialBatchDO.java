package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物资批次表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_batch")
public class MaterialBatchDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 批次编号，全局唯一 */
    private String batchNo;
    /** 关联物资ID */
    private Long materialId;
    /** 入库数量 */
    private Integer quantity;
    /** 在库余量 */
    private Integer remaining;
    /** 有效期至 */
    private LocalDate expiryDate;
    /** 入库时间 */
    private LocalDateTime stockInTime;
    /** 批次状态：1在库可用 2已清空 3报废 */
    private Integer batchStatus;
    /** 入库操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
