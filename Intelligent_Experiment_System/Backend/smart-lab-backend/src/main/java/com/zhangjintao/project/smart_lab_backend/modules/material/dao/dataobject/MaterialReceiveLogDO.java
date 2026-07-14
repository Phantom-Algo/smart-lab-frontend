package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领用出库日志表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_receive_log")
public class MaterialReceiveLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联申请单ID */
    private Long applyId;
    /** 物资ID */
    private Long materialId;
    /** 本次领用总数量 */
    private Integer totalQuantity;
    /** 领用人ID（person_id） */
    private Long receiverId;
    /** 出库操作人ID（person_id） */
    private Long outOperatorId;
    /** 出库时间 */
    private LocalDateTime outTime;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
