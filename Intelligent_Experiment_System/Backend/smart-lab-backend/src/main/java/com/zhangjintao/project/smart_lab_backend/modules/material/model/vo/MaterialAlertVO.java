package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录视图对象 — FR-0008
 *
 * @author linbowei
 */
@Data
@Builder
public class MaterialAlertVO {
    private Long id;
    /** 物资ID */
    private Long materialId;
    /** 物资名称 */
    private String materialName;
    /** 关联批次ID */
    private Long batchId;
    /** 批次编号 */
    private String batchNo;
    /** 预警类型 */
    private Integer alertType;
    /** 预警类型描述 */
    private String alertTypeDesc;
    /** 预警内容 */
    private String alertContent;
    /** 处理状态 */
    private Integer status;
    /** 处理状态描述 */
    private String statusDesc;
    /** 处理人ID */
    private Long handledBy;
    /** 预警触发时间 */
    private LocalDateTime alertTime;
    /** 处理时间 */
    private LocalDateTime handledAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
