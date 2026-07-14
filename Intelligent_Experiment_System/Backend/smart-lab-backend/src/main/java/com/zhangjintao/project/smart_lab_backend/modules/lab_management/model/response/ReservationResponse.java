package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Value
@Builder
public class ReservationResponse {

	Long id;
	String reservationNo;
	Long labId;
	String labName;
	Long applicantId;
	String applicantName;
	LocalDate reservationDate;
	LocalTime startTime;
	LocalTime endTime;
	String purpose;
	String courseName;
	Integer reservationStatus;
	String reservationStatusDesc;
	Long approverId;
	String approverName;
	LocalDateTime approveTime;
	String approveComment;
	LocalDateTime checkInAt;
	LocalDateTime checkOutAt;
	String cancelReason;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
