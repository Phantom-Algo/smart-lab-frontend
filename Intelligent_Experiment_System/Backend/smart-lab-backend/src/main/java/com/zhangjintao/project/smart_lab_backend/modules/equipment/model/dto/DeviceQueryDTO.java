package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import lombok.Data;

/**
 * 设备查询DTO
 *
 * @author linbowei
 */
@Data
public class DeviceQueryDTO {
    /** 设备名称（模糊搜索） */
    private String name;
    /** 设备类型ID */
    private Long deviceTypeId;
    /** 所属实验室ID */
    private Long labId;
    /** 设备状态 */
    private Integer deviceStatus;
    /** 页码（从1开始） */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 10;
}
