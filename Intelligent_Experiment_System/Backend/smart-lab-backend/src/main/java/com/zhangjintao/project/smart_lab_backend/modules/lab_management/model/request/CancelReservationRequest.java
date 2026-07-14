package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelReservationRequest {

	@NotBlank(message = "取消原因不能为空")
	@Size(max = 255, message = "取消原因长度不能超过255")
	private String cancelReason;
}
