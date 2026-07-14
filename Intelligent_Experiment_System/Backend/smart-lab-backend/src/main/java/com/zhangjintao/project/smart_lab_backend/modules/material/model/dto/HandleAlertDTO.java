package com.zhangjintao.project.smart_lab_backend.modules.material.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理预警DTO — FR-0008 handleAlert
 *
 * @author linbowei
 */
@Data
public class HandleAlertDTO {
    /** 处理结果（可选） */
    @Size(max = 255)
    private String result;
}
