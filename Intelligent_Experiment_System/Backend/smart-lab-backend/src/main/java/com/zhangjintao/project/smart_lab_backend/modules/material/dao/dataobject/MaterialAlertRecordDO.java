package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material_alert_record")
public class MaterialAlertRecordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 物资ID */
    private Long materialId;
    /** 关联批次ID（低库存预警时为NULL） */
    private Long batchId;
    /** 预警类型：1低库存 2临期 3过期 4超量领用 */
    private Integer alertType;
    /** 预警内容 */
    private String alertContent;
    /** 处理状态：0待处理 1已处理 */
    private Integer status;
    /** 处理人ID（person_id） */
    private Long handledBy;
    /** 预警触发时间 */
    private LocalDateTime alertTime;
    /** 处理时间 */
    private LocalDateTime handledAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
