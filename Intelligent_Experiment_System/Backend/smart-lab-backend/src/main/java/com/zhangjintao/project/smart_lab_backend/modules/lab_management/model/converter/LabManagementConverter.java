package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleItemCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleSaveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationApproveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCancelCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.ApproveReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CancelReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.LabCloseScheduleQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.LabPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.MyReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.PendingReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.SetLabSchedulesRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabStatusRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ConflictedReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;

import java.util.List;

public final class LabManagementConverter {

	private LabManagementConverter() {
	}

	public static LabCreateCommand toCreateCommand(CreateLabRequest request) {
		return LabCreateCommand.builder()
				.labName(request.getLabName())
				.labTypeId(request.getLabTypeId())
				.location(toLocation(request.getCampus(), request.getBuilding(), request.getFloor(), request.getRoomNo()))
				.capacity(request.getCapacity())
				.areaM2(request.getAreaM2())
				.dutyUserId(request.getDutyUserId())
				.description(request.getDescription())
				.build();
	}

	public static LabUpdateCommand toUpdateCommand(UpdateLabRequest request) {
		return LabUpdateCommand.builder()
				.labName(request.getLabName())
				.labTypeId(request.getLabTypeId())
				.location(hasLocationField(request) ? toLocation(request.getCampus(), request.getBuilding(),
						request.getFloor(), request.getRoomNo()) : null)
				.capacity(request.getCapacity())
				.areaM2(request.getAreaM2())
				.dutyUserId(request.getDutyUserId())
				.description(request.getDescription())
				.build();
	}

	public static LabQueryCriteria toCriteria(LabPageQueryRequest request) {
		return LabQueryCriteria.builder()
				.keyword(request.getKeyword())
				.campus(request.getCampus())
				.building(request.getBuilding())
				.floor(request.getFloor())
				.roomNo(request.getRoomNo())
				.labTypeId(request.getLabTypeId())
				.runningStatus(request.getRunningStatus())
				.openStatus(request.getOpenStatus())
				.page(request.getPage())
				.pageSize(request.getPageSize())
				.sortField(request.getSortField())
				.sortOrder(request.getSortOrder())
				.build();
	}

	public static LabStatusUpdateCommand toStatusUpdateCommand(UpdateLabStatusRequest request, Long operatorId) {
		return LabStatusUpdateCommand.builder()
				.runningStatus(request.getRunningStatus())
				.openStatus(request.getOpenStatus())
				.changeReason(request.getChangeReason())
				.operatorId(operatorId)
				.forceFinishCurrentUsage(request.forceFinishCurrentUsage())
				.forceCancelFutureReservations(request.forceCancelFutureReservations())
				.confirmPermanentClose(request.confirmPermanentClose())
				.build();
	}

	public static LabTypeCreateCommand toCreateCommand(CreateLabTypeRequest request) {
		return LabTypeCreateCommand.builder()
				.labTypeName(request.getLabTypeName().trim())
				.build();
	}

	public static LabTypeUpdateCommand toUpdateCommand(UpdateLabTypeRequest request) {
		return LabTypeUpdateCommand.builder()
				.labTypeName(request.getLabTypeName().trim())
				.build();
	}

	public static DetectionIndicatorCreateCommand toCreateCommand(CreateDetectionIndicatorRequest request) {
		return DetectionIndicatorCreateCommand.builder()
				.labTypeId(request.getLabTypeId())
				.indicatorName(request.getIndicatorName().trim())
				.unit(request.getUnit().trim())
				.safeRange(request.getSafeRange())
				.build();
	}

	public static DetectionIndicatorUpdateCommand toUpdateCommand(UpdateDetectionIndicatorRequest request) {
		return DetectionIndicatorUpdateCommand.builder()
				.labTypeId(request.getLabTypeId())
				.indicatorName(request.getIndicatorName() == null ? null : request.getIndicatorName().trim())
				.unit(request.getUnit() == null ? null : request.getUnit().trim())
				.safeRange(request.getSafeRange())
				.build();
	}

	public static ReservationCreateCommand toCreateCommand(CreateReservationRequest request, Long applicantId) {
		return ReservationCreateCommand.builder()
				.labId(request.getLabId())
				.applicantId(applicantId)
				.reservationDate(request.getReservationDate())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.purpose(request.getPurpose())
				.courseName(request.getCourseName())
				.build();
	}

	public static ReservationApproveCommand toApproveCommand(String reservationNo, ApproveReservationRequest request,
			Long approverId) {
		return ReservationApproveCommand.builder()
				.reservationNo(reservationNo)
				.approverId(approverId)
				.approved(request.getApproved())
				.approveComment(request.getApproveComment())
				.build();
	}

	public static ReservationCancelCommand toCancelCommand(String reservationNo, CancelReservationRequest request,
			Long applicantId) {
		return ReservationCancelCommand.builder()
				.reservationNo(reservationNo)
				.applicantId(applicantId)
				.cancelReason(request.getCancelReason())
				.build();
	}

	public static MyReservationQueryCriteria toCriteria(MyReservationPageQueryRequest request, Long applicantId) {
		return MyReservationQueryCriteria.builder()
				.applicantId(applicantId)
				.status(request.getStatus())
				.page(request.getPage())
				.pageSize(request.getPageSize())
				.build();
	}

	public static MyReservationQueryCriteria toCriteria(MyReservationPageQueryRequest request) {
		return MyReservationQueryCriteria.builder()
				.status(request.getStatus())
				.page(request.getPage())
				.pageSize(request.getPageSize())
				.build();
	}

	public static PendingReservationQueryCriteria toCriteria(PendingReservationPageQueryRequest request) {
		return PendingReservationQueryCriteria.builder()
				.labId(request.getLabId())
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.page(request.getPage())
				.pageSize(request.getPageSize())
				.build();
	}

	public static LabScheduleSaveCommand toSaveCommand(SetLabSchedulesRequest request) {
		return LabScheduleSaveCommand.builder()
				.schedules(request.getSchedules().stream()
						.map(item -> LabScheduleItemCommand.builder()
								.dayOfWeek(item.getDayOfWeek())
								.openTime(item.getOpenTime())
								.closeTime(item.getCloseTime())
								.build())
						.toList())
				.forceCancelConflictedReservations(request.forceCancelConflictedReservations())
				.build();
	}

	public static LabCloseScheduleCreateCommand toCreateCommand(CreateLabCloseScheduleRequest request) {
		return LabCloseScheduleCreateCommand.builder()
				.closeType(request.getCloseType())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.reason(request.getReason())
				.forceCancelConflictedReservations(request.forceCancelConflictedReservations())
				.build();
	}

	public static LabCloseScheduleUpdateCommand toUpdateCommand(UpdateLabCloseScheduleRequest request) {
		return LabCloseScheduleUpdateCommand.builder()
				.closeType(request.getCloseType())
				.startTime(request.getStartTime())
				.endTime(request.getEndTime())
				.reason(request.getReason())
				.forceCancelConflictedReservations(request.forceCancelConflictedReservations())
				.build();
	}

	public static LabCloseScheduleQueryCriteria toCriteria(LabCloseScheduleQueryRequest request) {
		return LabCloseScheduleQueryCriteria.builder()
				.startDate(request.getStartDate())
				.endDate(request.getEndDate())
				.closeType(request.getCloseType())
				.build();
	}

	public static LabResponse toLabResponse(LabPageItemDTO item, String labTypeName, String dutyUserName) {
		LabLocation location = item.getLocation();
		return LabResponse.builder()
				.id(item.getId())
				.labName(item.getLabName())
				.labTypeId(item.getLabTypeId())
				.labTypeName(labTypeName)
				.campus(location == null ? null : location.getCampus())
				.building(location == null ? null : location.getBuilding())
				.floor(location == null ? null : location.getFloor())
				.roomNo(location == null ? null : location.getRoomNo())
				.capacity(item.getCapacity())
				.areaM2(item.getAreaM2())
				.dutyUserId(item.getDutyUserId())
				.dutyUserName(dutyUserName)
				.runningStatus(item.getRunningStatus() == null ? null : item.getRunningStatus().getCode())
				.runningStatusDesc(item.getRunningStatus() == null ? null : item.getRunningStatus().getDescription())
				.openStatus(item.getOpenStatus() == null ? null : item.getOpenStatus().getCode())
				.openStatusDesc(item.getOpenStatus() == null ? null : item.getOpenStatus().getDescription())
				.description(item.getDescription())
				.createdAt(item.getCreatedAt())
				.updatedAt(item.getUpdatedAt())
				.build();
	}

	public static LabDetailResponse toLabDetailResponse(Lab lab, String labTypeName, String dutyUserName,
			List<LabWeeklySchedule> schedules) {
		LabLocation location = lab.getLocation();
		return LabDetailResponse.builder()
				.id(lab.getId())
				.labName(lab.getLabName())
				.labTypeId(lab.getLabTypeId())
				.labTypeName(labTypeName)
				.campus(location == null ? null : location.getCampus())
				.building(location == null ? null : location.getBuilding())
				.floor(location == null ? null : location.getFloor())
				.roomNo(location == null ? null : location.getRoomNo())
				.capacity(lab.getCapacity())
				.areaM2(lab.getAreaM2())
				.dutyUserId(lab.getDutyUserId())
				.dutyUserName(dutyUserName)
				.runningStatus(lab.getRunningStatus() == null ? null : lab.getRunningStatus().getCode())
				.runningStatusDesc(lab.getRunningStatus() == null ? null : lab.getRunningStatus().getDescription())
				.openStatus(lab.getOpenStatus() == null ? null : lab.getOpenStatus().getCode())
				.openStatusDesc(lab.getOpenStatus() == null ? null : lab.getOpenStatus().getDescription())
				.description(lab.getDescription())
				.createdAt(lab.getCreatedAt())
				.updatedAt(lab.getUpdatedAt())
				.weeklySchedules(schedules.stream().map(LabManagementConverter::toScheduleResponse).toList())
				.build();
	}

	public static LabTypeResponse toLabTypeResponse(LabType labType) {
		return LabTypeResponse.builder()
				.id(labType.getId())
				.labTypeName(labType.getLabTypeName())
				.createdAt(labType.getCreatedAt())
				.updatedAt(labType.getUpdatedAt())
				.build();
	}

	public static DetectionIndicatorResponse toDetectionIndicatorResponse(DetectionIndicator indicator) {
		return DetectionIndicatorResponse.builder()
				.id(indicator.getId())
				.labTypeId(indicator.getLabTypeId())
				.indicatorName(indicator.getIndicatorName())
				.unit(indicator.getUnit())
				.safeRange(indicator.getSafeRange() == null ? null : indicator.getSafeRange().toExternalValue())
				.createdAt(indicator.getCreatedAt())
				.updatedAt(indicator.getUpdatedAt())
				.build();
	}

	public static ReservationResponse toReservationResponse(LabReservation reservation, String labName,
			String applicantName, String approverName) {
		return ReservationResponse.builder()
				.id(reservation.getId())
				.reservationNo(reservation.getReservationNo())
				.labId(reservation.getLabId())
				.labName(labName)
				.applicantId(reservation.getApplicantId())
				.applicantName(applicantName)
				.reservationDate(reservation.getReservationDate())
				.startTime(reservation.getStartTime())
				.endTime(reservation.getEndTime())
				.purpose(reservation.getPurpose())
				.courseName(reservation.getCourseName())
				.reservationStatus(reservation.getReservationStatus().getCode())
				.reservationStatusDesc(reservation.getReservationStatus().getDescription())
				.approverId(reservation.getApproverId())
				.approverName(approverName)
				.approveTime(reservation.getApproveTime())
				.approveComment(reservation.getApproveComment())
				.checkInAt(reservation.getCheckInAt())
				.checkOutAt(reservation.getCheckOutAt())
				.cancelReason(reservation.getCancelReason())
				.createdAt(reservation.getCreatedAt())
				.updatedAt(reservation.getUpdatedAt())
				.build();
	}

	public static LabOccupancyResponse.OccupiedTime toOccupiedTime(LabReservation reservation) {
		return LabOccupancyResponse.OccupiedTime.builder()
				.reservationNo(reservation.getReservationNo())
				.startTime(reservation.getStartTime())
				.endTime(reservation.getEndTime())
				.reservationStatus(reservation.getReservationStatus().getCode())
				.reservationStatusDesc(reservation.getReservationStatus().getDescription())
				.build();
	}

	public static LabScheduleResponse toScheduleResponse(LabWeeklySchedule schedule) {
		return LabScheduleResponse.builder()
				.id(schedule.getId())
				.dayOfWeek(schedule.getDayOfWeek())
				.openTime(schedule.getOpenTime())
				.closeTime(schedule.getCloseTime())
				.build();
	}

	public static LabCloseScheduleResponse toCloseScheduleResponse(LabCloseSchedule schedule) {
		return LabCloseScheduleResponse.builder()
				.id(schedule.getId())
				.labId(schedule.getLabId())
				.closeType(schedule.getCloseType() == null ? null : schedule.getCloseType().getCode())
				.closeTypeDesc(schedule.getCloseType() == null ? null : schedule.getCloseType().getDescription())
				.startTime(schedule.getStartTime())
				.endTime(schedule.getEndTime())
				.reason(schedule.getReason())
				.createdAt(schedule.getCreatedAt())
				.updatedAt(schedule.getUpdatedAt())
				.build();
	}

	public static ConflictedReservationResponse toConflictedReservationResponse(LabReservation reservation) {
		return ConflictedReservationResponse.builder()
				.reservationNo(reservation.getReservationNo())
				.applicantId(reservation.getApplicantId())
				.reservationDate(reservation.getReservationDate())
				.startTime(reservation.getStartTime())
				.endTime(reservation.getEndTime())
				.reservationStatus(reservation.getReservationStatus().getCode())
				.reservationStatusDesc(reservation.getReservationStatus().getDescription())
				.build();
	}

	private static LabLocation toLocation(String campus, String building, Integer floor, String roomNo) {
		return LabLocation.builder()
				.campus(campus)
				.building(building)
				.floor(floor)
				.roomNo(roomNo)
				.build();
	}

	private static boolean hasLocationField(UpdateLabRequest request) {
		return request.getCampus() != null
				|| request.getBuilding() != null
				|| request.getFloor() != null
				|| request.getRoomNo() != null;
	}
}
