package com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备故障报修DTO
 *
 * @author linbowei
 */
@Data
public class RepairReportDTO {
    /** 报修人ID（Controller从SecurityContext注入） */
    @NotNull(message = "报修人ID不能为空")
    private Long reporterId;
    /** 设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    /** 故障描述 */
    @NotBlank(message = "故障描述不能为空")
    private String faultDesc;
}
