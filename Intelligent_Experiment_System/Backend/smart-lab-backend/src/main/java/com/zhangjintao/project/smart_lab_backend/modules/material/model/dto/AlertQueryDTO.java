package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import lombok.Data;

/**
 * 预警查询DTO — FR-0008
 *
 * @author linbowei
 */
@Data
public class AlertQueryDTO {
    /** 物资ID */
    private Long materialId;
    /** 预警类型：1低库存 2临期 3过期 4超量领用 */
    private Integer alertType;
    /** 处理状态：0待处理 1已处理 */
    private Integer status;
    /** 页码 */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 20;
}
