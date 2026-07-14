package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
@Builder
public class ReservationCreateCommand {

	Long labId;
	Long applicantId;
	LocalDate reservationDate;
	LocalTime startTime;
	LocalTime endTime;
	String purpose;
	String courseName;
}
