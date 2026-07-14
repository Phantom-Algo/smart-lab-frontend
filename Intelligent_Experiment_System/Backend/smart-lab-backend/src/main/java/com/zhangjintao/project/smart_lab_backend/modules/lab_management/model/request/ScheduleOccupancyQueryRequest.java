package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ScheduleOccupancyQueryRequest {

	@NotNull(message = "查询日期不能为空")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;
}
