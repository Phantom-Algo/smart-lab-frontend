package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

/**
 * 维修记录查询DTO
 *
 * @author linbowei
 */
@Data
public class RepairQueryDTO {
    /** 设备ID */
    private Long deviceId;
    /** 报修人ID */
    private Long reporterId;
    /** 维修状态 */
    private Integer repairStatus;
    /** 页码 */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 10;
}
