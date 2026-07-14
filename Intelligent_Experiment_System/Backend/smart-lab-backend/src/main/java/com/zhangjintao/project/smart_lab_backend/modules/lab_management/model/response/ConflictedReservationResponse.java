package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
@Builder
public class ConflictedReservationResponse {

	String reservationNo;
	Long applicantId;
	LocalDate reservationDate;
	LocalTime startTime;
	LocalTime endTime;
	Integer reservationStatus;
	String reservationStatusDesc;
}
