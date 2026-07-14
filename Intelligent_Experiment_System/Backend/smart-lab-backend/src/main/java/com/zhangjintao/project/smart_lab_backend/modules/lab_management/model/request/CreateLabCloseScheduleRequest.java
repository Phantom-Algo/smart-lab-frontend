package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLabCloseScheduleRequest {

	@NotNull(message = "关闭类型不能为空")
	@Min(value = 1, message = "关闭类型只能是1到4")
	@Max(value = 4, message = "关闭类型只能是1到4")
	private Integer closeType;

	@NotNull(message = "不开放开始时间不能为空")
	private LocalDateTime startTime;

	@NotNull(message = "不开放结束时间不能为空")
	private LocalDateTime endTime;

	@Size(max = 255, message = "不开放原因长度不能超过255")
	private String reason;

	private Boolean forceCancelConflictedReservations = false;

	public boolean forceCancelConflictedReservations() {
		return Boolean.TRUE.equals(forceCancelConflictedReservations);
	}
}
