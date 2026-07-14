package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class LabCloseScheduleQueryRequest {

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate startDate;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate endDate;

	@Min(value = 1, message = "关闭类型只能是1到4")
	@Max(value = 4, message = "关闭类型只能是1到4")
	private Integer closeType;
}
