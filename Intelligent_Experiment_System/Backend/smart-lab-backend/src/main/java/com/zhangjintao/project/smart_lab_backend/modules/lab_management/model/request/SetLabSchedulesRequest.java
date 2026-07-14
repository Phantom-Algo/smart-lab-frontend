package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class SetLabSchedulesRequest {

	@NotEmpty(message = "开放时间数组不能为空")
	@Size(max = 7, message = "开放时间最多只能设置7天")
	private List<@Valid ScheduleItem> schedules;

	private Boolean forceCancelConflictedReservations = false;

	public boolean forceCancelConflictedReservations() {
		return Boolean.TRUE.equals(forceCancelConflictedReservations);
	}

	@Data
	public static class ScheduleItem {

		@NotNull(message = "星期不能为空")
		@Min(value = 1, message = "星期只能是1到7")
		@Max(value = 7, message = "星期只能是1到7")
		private Integer dayOfWeek;

		@NotNull(message = "开放时间不能为空")
		private LocalTime openTime;

		@NotNull(message = "关闭时间不能为空")
		private LocalTime closeTime;
	}
}
