package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateReservationRequest {

	@NotNull(message = "实验室ID不能为空")
	@Positive(message = "实验室ID必须为正数")
	private Long labId;

	@NotNull(message = "预约日期不能为空")
	private LocalDate reservationDate;

	@NotNull(message = "开始时间不能为空")
	private LocalTime startTime;

	@NotNull(message = "结束时间不能为空")
	private LocalTime endTime;

	@Size(max = 512, message = "实验内容/目的长度不能超过512")
	private String purpose;

	@Size(max = 128, message = "课程名称长度不能超过128")
	private String courseName;
}
