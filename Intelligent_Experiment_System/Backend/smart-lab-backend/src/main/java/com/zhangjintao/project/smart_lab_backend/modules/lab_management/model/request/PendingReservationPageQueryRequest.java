package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PendingReservationPageQueryRequest {

	@Positive(message = "实验室ID必须为正数")
	private Long labId;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate startDate;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate endDate;

	@Min(value = 1, message = "页码必须大于等于1")
	private Long page = LabManagementConstants.DEFAULT_PAGE;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 100, message = "每页条数不能超过100")
	private Long pageSize = LabManagementConstants.DEFAULT_PAGE_SIZE;
}
