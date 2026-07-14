package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领用申请表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_receive_apply")
public class MaterialReceiveApplyDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 申请单号，全局唯一 */
    private String applyNo;
    /** 物资ID */
    private Long materialId;
    /** 申请人ID（person_id） */
    private Long applicantId;
    /** 审批人ID（person_id），审批时填入 */
    private Long approverId;
    /** 申请领用数量 */
    private Integer expectedQuantity;
    /** 物资用途 */
    private String purpose;
    /** 申请状态：1待审核 2审批通过 3驳回 4已取消 */
    private Integer applyStatus;
    /** 驳回原因 */
    private String rejectReason;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
