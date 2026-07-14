package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLabStatusRequest {

	@Min(value = 1, message = "使用状态只能是1到4")
	@Max(value = 4, message = "使用状态只能是1到4")
	private Integer runningStatus;

	@Min(value = 1, message = "开放状态只能是1到3")
	@Max(value = 3, message = "开放状态只能是1到3")
	private Integer openStatus;

	@Size(max = 255, message = "状态变更原因长度不能超过255")
	private String changeReason;

	private Boolean forceFinishCurrentUsage = false;

	private Boolean forceCancelFutureReservations = false;

	private Boolean confirmPermanentClose = false;

	public boolean forceFinishCurrentUsage() {
		return Boolean.TRUE.equals(forceFinishCurrentUsage);
	}

	public boolean forceCancelFutureReservations() {
		return Boolean.TRUE.equals(forceCancelFutureReservations);
	}

	public boolean confirmPermanentClose() {
		return Boolean.TRUE.equals(confirmPermanentClose);
	}
}
