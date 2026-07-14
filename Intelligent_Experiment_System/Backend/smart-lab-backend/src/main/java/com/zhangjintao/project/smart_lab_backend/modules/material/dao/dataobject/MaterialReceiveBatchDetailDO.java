package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库批次扣减明细表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_receive_batch_detail")
public class MaterialReceiveBatchDetailDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联出库日志ID */
    private Long receiveLogId;
    /** 被扣减的批次ID */
    private Long batchId;
    /** 本批次扣减数量 */
    private Integer deductedQuantity;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
