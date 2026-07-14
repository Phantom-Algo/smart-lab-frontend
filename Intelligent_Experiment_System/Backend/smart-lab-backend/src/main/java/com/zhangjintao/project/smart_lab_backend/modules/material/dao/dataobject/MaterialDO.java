package com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物资表 DO
 *
 * @author linbowei
 */
@Data
@TableName("material")
public class MaterialDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 物资编号，全局唯一 */
    private String materialNo;
    /** 物资名称 */
    private String materialName;
    /** 所属实验室ID，逻辑外键→lab_management.lab.id */
    private Long labId;
    /** 计量单位 */
    private String unit;
    /** 安全库存量 */
    private Integer safetyStock;
    /** 当前在库总量 */
    private Integer currentStock;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
