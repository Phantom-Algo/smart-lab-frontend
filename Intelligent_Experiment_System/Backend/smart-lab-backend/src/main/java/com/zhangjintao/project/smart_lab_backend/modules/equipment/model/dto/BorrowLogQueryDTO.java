package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

/**
 * 借用日志查询DTO
 *
 * @author linbowei
 */
@Data
public class BorrowLogQueryDTO {
    /** 设备ID */
    private Long deviceId;
    /** 借用人ID */
    private Long borrowerId;
    /** 借用状态 */
    private Integer status;
    /** 页码 */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 10;
}
