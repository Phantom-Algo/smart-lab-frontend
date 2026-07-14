package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveReservationRequest {

	@NotNull(message = "审核结果不能为空")
	private Boolean approved;

	@Size(max = 255, message = "审核意见长度不能超过255")
	private String approveComment;
}
