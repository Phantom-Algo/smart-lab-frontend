package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReservation {

	private Long id;
	private String reservationNo;
	private Long labId;
	private Long applicantId;
	private LocalDate reservationDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private String purpose;
	private String courseName;
	private LabReservationStatus reservationStatus;
	private Long approverId;
	private LocalDateTime approveTime;
	private String approveComment;
	private LocalDateTime checkInAt;
	private LocalDateTime checkOutAt;
	private String cancelReason;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void cancelByLabDeleted(String reason, LocalDateTime now) {
		this.reservationStatus = LabReservationStatus.CANCELLED;
		this.cancelReason = reason;
		this.updatedAt = now;
	}

	public void cancelBySystem(String reason, LocalDateTime now) {
		this.reservationStatus = LabReservationStatus.CANCELLED;
		this.cancelReason = reason;
		this.updatedAt = now;
	}

	public void approve(Long approverId, String approveComment, LocalDateTime now) {
		this.approverId = approverId;
		this.approveTime = now;
		this.approveComment = approveComment;
		this.reservationStatus = LabReservationStatus.APPROVED;
		this.updatedAt = now;
	}

	public void reject(Long approverId, String approveComment, LocalDateTime now) {
		this.approverId = approverId;
		this.approveTime = now;
		this.approveComment = approveComment;
		this.reservationStatus = LabReservationStatus.REJECTED;
		this.updatedAt = now;
	}

	public void cancelByApplicant(String reason, LocalDateTime now) {
		this.reservationStatus = LabReservationStatus.CANCELLED;
		this.cancelReason = reason;
		this.updatedAt = now;
	}

	public void checkIn(LocalDateTime now) {
		this.reservationStatus = LabReservationStatus.CHECKED_IN;
		this.checkInAt = now;
		this.updatedAt = now;
	}

	public void checkOut(LocalDateTime now) {
		this.checkOutAt = now;
		this.updatedAt = now;
	}

	public void forceCheckOut(LocalDateTime now) {
		this.checkOutAt = now;
		this.updatedAt = now;
	}

	public void markAbsent(LocalDateTime now) {
		this.reservationStatus = LabReservationStatus.ABSENT;
		this.updatedAt = now;
	}

	public boolean belongsToApplicant(Long applicantId) {
		return Objects.equals(this.applicantId, applicantId);
	}
}
