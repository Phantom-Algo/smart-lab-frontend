package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabCloseScheduleDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDetectionLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.DetectionIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabReservationDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabScheduleDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabStatusChangeLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabStatusChangeLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabCloseType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;

import java.math.BigDecimal;
import java.util.List;

final class LabPersistenceConverter {

	private static final TypeReference<List<List<BigDecimal>>> SAFE_RANGE_TYPE = new TypeReference<>() {
	};

	private LabPersistenceConverter() {
	}

	static Lab toLab(LabDO labDO) {
		if (labDO == null) {
			return null;
		}
		return Lab.builder()
				.id(labDO.getId())
				.labName(labDO.getLabName())
				.labTypeId(labDO.getLabTypeId())
				.location(toLocation(labDO))
				.capacity(labDO.getCapacity())
				.areaM2(labDO.getAreaM2())
				.dutyUserId(labDO.getDutyUserId())
				.runningStatus(labDO.getRunningStatus() == null ? null : LabRunningStatus.fromCode(labDO.getRunningStatus()))
				.openStatus(labDO.getOpenStatus() == null ? null : LabOpenStatus.fromCode(labDO.getOpenStatus()))
				.description(labDO.getDescription())
				.createdAt(labDO.getCreatedAt())
				.updatedAt(labDO.getUpdatedAt())
				.deletedAt(labDO.getDeletedAt())
				.build();
	}

	static LabDO toLabDO(Lab lab) {
		LabDO labDO = new LabDO();
		labDO.setId(lab.getId());
		labDO.setLabName(lab.getLabName());
		labDO.setLabTypeId(lab.getLabTypeId());
		LabLocation location = lab.getLocation();
		if (location != null) {
			labDO.setCampus(location.getCampus());
			labDO.setBuilding(location.getBuilding());
			labDO.setFloor(location.getFloor());
			labDO.setRoomNo(location.getRoomNo());
		}
		labDO.setCapacity(lab.getCapacity());
		labDO.setAreaM2(lab.getAreaM2());
		labDO.setDutyUserId(lab.getDutyUserId());
		labDO.setRunningStatus(lab.getRunningStatus() == null ? null : lab.getRunningStatus().getCode());
		labDO.setOpenStatus(lab.getOpenStatus() == null ? null : lab.getOpenStatus().getCode());
		labDO.setDescription(lab.getDescription());
		labDO.setCreatedAt(lab.getCreatedAt());
		labDO.setUpdatedAt(lab.getUpdatedAt());
		labDO.setDeletedAt(lab.getDeletedAt());
		return labDO;
	}

	static LabPageItemDTO toLabPageItemDTO(LabDO labDO) {
		return LabPageItemDTO.builder()
				.id(labDO.getId())
				.labName(labDO.getLabName())
				.labTypeId(labDO.getLabTypeId())
				.location(toLocation(labDO))
				.capacity(labDO.getCapacity())
				.areaM2(labDO.getAreaM2())
				.dutyUserId(labDO.getDutyUserId())
				.runningStatus(LabRunningStatus.fromCode(labDO.getRunningStatus()))
				.openStatus(LabOpenStatus.fromCode(labDO.getOpenStatus()))
				.description(labDO.getDescription())
				.createdAt(labDO.getCreatedAt())
				.updatedAt(labDO.getUpdatedAt())
				.build();
	}

	static LabType toLabType(LabTypeDO labTypeDO) {
		if (labTypeDO == null) {
			return null;
		}
		return LabType.builder()
				.id(labTypeDO.getId())
				.labTypeName(labTypeDO.getLabTypeName())
				.createdAt(labTypeDO.getCreatedAt())
				.updatedAt(labTypeDO.getUpdatedAt())
				.build();
	}

	static LabTypeDO toLabTypeDO(LabType labType) {
		LabTypeDO labTypeDO = new LabTypeDO();
		labTypeDO.setId(labType.getId());
		labTypeDO.setLabTypeName(labType.getLabTypeName());
		labTypeDO.setCreatedAt(labType.getCreatedAt());
		labTypeDO.setUpdatedAt(labType.getUpdatedAt());
		return labTypeDO;
	}

	static DetectionIndicator toDetectionIndicator(DetectionIndicatorDO indicatorDO, ObjectMapper objectMapper) {
		if (indicatorDO == null) {
			return null;
		}
		return DetectionIndicator.builder()
				.id(indicatorDO.getId())
				.labTypeId(indicatorDO.getLabTypeId())
				.indicatorName(indicatorDO.getIndicatorName())
				.unit(indicatorDO.getUnit())
				.safeRange(readSafeRange(indicatorDO.getSafeRange(), objectMapper))
				.createdAt(indicatorDO.getCreatedAt())
				.updatedAt(indicatorDO.getUpdatedAt())
				.build();
	}

	static DetectionIndicatorDO toDetectionIndicatorDO(DetectionIndicator indicator, ObjectMapper objectMapper) {
		DetectionIndicatorDO indicatorDO = new DetectionIndicatorDO();
		indicatorDO.setId(indicator.getId());
		indicatorDO.setLabTypeId(indicator.getLabTypeId());
		indicatorDO.setIndicatorName(indicator.getIndicatorName());
		indicatorDO.setUnit(indicator.getUnit());
		indicatorDO.setSafeRange(writeSafeRange(indicator.getSafeRange(), objectMapper));
		indicatorDO.setCreatedAt(indicator.getCreatedAt());
		indicatorDO.setUpdatedAt(indicator.getUpdatedAt());
		return indicatorDO;
	}

	private static DetectionSafeRange readSafeRange(String safeRangeJson, ObjectMapper objectMapper) {
		if (safeRangeJson == null) {
			return null;
		}
		try {
			List<List<BigDecimal>> rawSegments = objectMapper.readValue(safeRangeJson, SAFE_RANGE_TYPE);
			return DetectionSafeRange.builder()
					.segments(rawSegments.stream()
							.map(segment -> DetectionSafeRangeSegment.builder()
									.min(segment.get(0))
									.max(segment.get(1))
									.build())
							.toList())
					.build();
		}
		catch (JsonProcessingException | IndexOutOfBoundsException ex) {
			throw new IllegalArgumentException("实验室检测指标安全区间 JSON 反序列化失败", ex);
		}
	}

	private static String writeSafeRange(DetectionSafeRange safeRange, ObjectMapper objectMapper) {
		if (safeRange == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(safeRange.toExternalValue());
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("实验室检测指标安全区间 JSON 序列化失败", ex);
		}
	}

	static LabWeeklySchedule toLabWeeklySchedule(LabScheduleDO labScheduleDO) {
		return LabWeeklySchedule.builder()
				.id(labScheduleDO.getId())
				.labId(labScheduleDO.getLabId())
				.dayOfWeek(labScheduleDO.getDayOfWeek())
				.openTime(labScheduleDO.getOpenTime())
				.closeTime(labScheduleDO.getCloseTime())
				.build();
	}

	static LabScheduleDO toLabScheduleDO(LabWeeklySchedule schedule) {
		LabScheduleDO scheduleDO = new LabScheduleDO();
		scheduleDO.setId(schedule.getId());
		scheduleDO.setLabId(schedule.getLabId());
		scheduleDO.setDayOfWeek(schedule.getDayOfWeek());
		scheduleDO.setOpenTime(schedule.getOpenTime());
		scheduleDO.setCloseTime(schedule.getCloseTime());
		return scheduleDO;
	}

	static LabCloseSchedule toLabCloseSchedule(LabCloseScheduleDO closeScheduleDO) {
		if (closeScheduleDO == null) {
			return null;
		}
		return LabCloseSchedule.builder()
				.id(closeScheduleDO.getId())
				.labId(closeScheduleDO.getLabId())
				.closeType(LabCloseType.fromCode(closeScheduleDO.getCloseType()))
				.startTime(closeScheduleDO.getStartTime())
				.endTime(closeScheduleDO.getEndTime())
				.reason(closeScheduleDO.getReason())
				.createdAt(closeScheduleDO.getCreatedAt())
				.updatedAt(closeScheduleDO.getUpdatedAt())
				.build();
	}

	static LabCloseScheduleDO toLabCloseScheduleDO(LabCloseSchedule closeSchedule) {
		LabCloseScheduleDO closeScheduleDO = new LabCloseScheduleDO();
		closeScheduleDO.setId(closeSchedule.getId());
		closeScheduleDO.setLabId(closeSchedule.getLabId());
		closeScheduleDO.setCloseType(closeSchedule.getCloseType() == null ? null : closeSchedule.getCloseType().getCode());
		closeScheduleDO.setStartTime(closeSchedule.getStartTime());
		closeScheduleDO.setEndTime(closeSchedule.getEndTime());
		closeScheduleDO.setReason(closeSchedule.getReason());
		closeScheduleDO.setCreatedAt(closeSchedule.getCreatedAt());
		closeScheduleDO.setUpdatedAt(closeSchedule.getUpdatedAt());
		return closeScheduleDO;
	}

	static LabReservation toLabReservation(LabReservationDO reservationDO) {
		return LabReservation.builder()
				.id(reservationDO.getId())
				.reservationNo(reservationDO.getReservationNo())
				.labId(reservationDO.getLabId())
				.applicantId(reservationDO.getApplicantId())
				.reservationDate(reservationDO.getReservationDate())
				.startTime(reservationDO.getStartTime())
				.endTime(reservationDO.getEndTime())
				.purpose(reservationDO.getPurpose())
				.courseName(reservationDO.getCourseName())
				.reservationStatus(LabReservationStatus.fromCode(reservationDO.getReservationStatus()))
				.approverId(reservationDO.getApproverId())
				.approveTime(reservationDO.getApproveTime())
				.approveComment(reservationDO.getApproveComment())
				.checkInAt(reservationDO.getCheckInAt())
				.checkOutAt(reservationDO.getCheckOutAt())
				.cancelReason(reservationDO.getCancelReason())
				.createdAt(reservationDO.getCreatedAt())
				.updatedAt(reservationDO.getUpdatedAt())
				.build();
	}

	static LabReservationDO toReservationDO(LabReservation reservation) {
		LabReservationDO reservationDO = new LabReservationDO();
		reservationDO.setId(reservation.getId());
		reservationDO.setReservationNo(reservation.getReservationNo());
		reservationDO.setLabId(reservation.getLabId());
		reservationDO.setApplicantId(reservation.getApplicantId());
		reservationDO.setReservationDate(reservation.getReservationDate());
		reservationDO.setStartTime(reservation.getStartTime());
		reservationDO.setEndTime(reservation.getEndTime());
		reservationDO.setPurpose(reservation.getPurpose());
		reservationDO.setCourseName(reservation.getCourseName());
		reservationDO.setReservationStatus(reservation.getReservationStatus().getCode());
		reservationDO.setApproverId(reservation.getApproverId());
		reservationDO.setApproveTime(reservation.getApproveTime());
		reservationDO.setApproveComment(reservation.getApproveComment());
		reservationDO.setCheckInAt(reservation.getCheckInAt());
		reservationDO.setCheckOutAt(reservation.getCheckOutAt());
		reservationDO.setCancelReason(reservation.getCancelReason());
		reservationDO.setCreatedAt(reservation.getCreatedAt());
		reservationDO.setUpdatedAt(reservation.getUpdatedAt());
		return reservationDO;
	}

	static LabStatusChangeLogDO toLabStatusChangeLogDO(LabStatusChangeLog log) {
		LabStatusChangeLogDO logDO = new LabStatusChangeLogDO();
		logDO.setId(log.getId());
		logDO.setLabId(log.getLabId());
		logDO.setOperatorId(log.getOperatorId());
		logDO.setFromRunningStatus(log.getFromRunningStatus().getCode());
		logDO.setToRunningStatus(log.getToRunningStatus().getCode());
		logDO.setFromOpenStatus(log.getFromOpenStatus().getCode());
		logDO.setToOpenStatus(log.getToOpenStatus().getCode());
		logDO.setChangeReason(log.getChangeReason());
		logDO.setTriggerSource(log.getTriggerSource());
		logDO.setForcedCheckoutCount(log.getForcedCheckoutCount());
		logDO.setCancelledReservationCount(log.getCancelledReservationCount());
		logDO.setCreatedAt(log.getCreatedAt());
		return logDO;
	}

	static LabDetectionLog toLabDetectionLog(LabDetectionLogDO detectionLogDO) {
		if (detectionLogDO == null) {
			return null;
		}
		return LabDetectionLog.builder()
				.id(detectionLogDO.getId())
				.eventNo(detectionLogDO.getEventNo())
				.messageId(detectionLogDO.getMessageId())
				.labId(detectionLogDO.getLabId())
				.sensorId(detectionLogDO.getSensorId())
				.sensorNo(detectionLogDO.getSensorNo())
				.detectedData(detectionLogDO.getDetectedData())
				.detectedAt(detectionLogDO.getDetectedAt())
				.status(DetectionStatus.fromCode(detectionLogDO.getStatus()))
				.alertDetail(detectionLogDO.getAlertDetail())
				.createdAt(detectionLogDO.getCreatedAt())
				.build();
	}

	static LabDetectionLogDO toLabDetectionLogDO(LabDetectionLog detectionLog) {
		LabDetectionLogDO detectionLogDO = new LabDetectionLogDO();
		detectionLogDO.setId(detectionLog.getId());
		detectionLogDO.setEventNo(detectionLog.getEventNo());
		detectionLogDO.setMessageId(detectionLog.getMessageId());
		detectionLogDO.setLabId(detectionLog.getLabId());
		detectionLogDO.setSensorId(detectionLog.getSensorId());
		detectionLogDO.setSensorNo(detectionLog.getSensorNo());
		detectionLogDO.setDetectedData(detectionLog.getDetectedData());
		detectionLogDO.setDetectedAt(detectionLog.getDetectedAt());
		detectionLogDO.setStatus(detectionLog.getStatus().getCode());
		detectionLogDO.setAlertDetail(detectionLog.getAlertDetail());
		detectionLogDO.setCreatedAt(detectionLog.getCreatedAt());
		return detectionLogDO;
	}

	private static LabLocation toLocation(LabDO labDO) {
		return LabLocation.builder()
				.campus(labDO.getCampus())
				.building(labDO.getBuilding())
				.floor(labDO.getFloor())
				.roomNo(labDO.getRoomNo())
				.build();
	}
}
