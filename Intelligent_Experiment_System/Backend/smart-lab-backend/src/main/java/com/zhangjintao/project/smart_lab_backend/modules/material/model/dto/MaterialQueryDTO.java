package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import lombok.Data;

/**
 * 物资查询DTO
 *
 * @author linbowei
 */
@Data
public class MaterialQueryDTO {
    /** 物资名称（模糊搜索） */
    private String materialName;
    /** 所属实验室ID */
    private Long labId;
    /** 物资编号（精确匹配） */
    private String materialNo;
    /** 页码（从1开始） */
    private Integer pageNum = 1;
    /** 每页大小 */
    private Integer pageSize = 20;
}
