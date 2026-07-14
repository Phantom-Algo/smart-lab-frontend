package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabCloseScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabCloseType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ConflictedReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabCloseScheduleService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabCloseScheduleServiceImpl implements LabCloseScheduleService {

	private final LabRepository labRepository;
	private final LabCloseScheduleRepository labCloseScheduleRepository;
	private final LabReservationRepository labReservationRepository;
	private final LabOccupancyCacheManager labOccupancyCacheManager;
	private final MessagePublisher messagePublisher;

	@Override
	@Transactional
	public LabCloseScheduleMutationResponse create(Long labId, LabCloseScheduleCreateCommand command) {
		Lab lab = requireLabForUpdate(labId);
		LabCloseType closeType = parseCloseType(command.getCloseType());
		validateCloseTime(command.getStartTime(), command.getEndTime());
		String reason = normalizeReason(command.getReason());
		List<LabReservation> conflictedReservations = listConflicts(labId, command.getStartTime(), command.getEndTime());
		if (!conflictedReservations.isEmpty() && !command.isForceCancelConflictedReservations()) {
			return buildMutationResponse(false, null, conflictedReservations);
		}

		LabCloseSchedule savedSchedule = labCloseScheduleRepository.save(LabCloseSchedule.builder()
				.labId(labId)
				.closeType(closeType)
				.startTime(command.getStartTime())
				.endTime(command.getEndTime())
				.reason(reason)
				.build());
		cancelReservations(lab, conflictedReservations, buildCancelReason(closeType, reason));
		return buildMutationResponse(true, savedSchedule, conflictedReservations);
	}

	@Override
	@Transactional
	public LabCloseScheduleMutationResponse update(Long labId, Long scheduleId, LabCloseScheduleUpdateCommand command) {
		Lab lab = requireLabForUpdate(labId);
		LabCloseSchedule schedule = requireCloseSchedule(labId, scheduleId);
		LabCloseType closeType = parseCloseType(command.getCloseType());
		validateCloseTime(command.getStartTime(), command.getEndTime());
		String reason = normalizeReason(command.getReason());
		List<LabReservation> conflictedReservations = listConflicts(labId, command.getStartTime(), command.getEndTime());
		if (!conflictedReservations.isEmpty() && !command.isForceCancelConflictedReservations()) {
			return buildMutationResponse(false, schedule, conflictedReservations);
		}

		schedule.applyUpdate(closeType, command.getStartTime(), command.getEndTime(), reason);
		LabCloseSchedule updatedSchedule = labCloseScheduleRepository.update(schedule);
		cancelReservations(lab, conflictedReservations, buildCancelReason(closeType, reason));
		return buildMutationResponse(true, updatedSchedule, conflictedReservations);
	}

	@Override
	@Transactional
	public void delete(Long labId, Long scheduleId) {
		requireLabForUpdate(labId);
		requireCloseSchedule(labId, scheduleId);
		labCloseScheduleRepository.deleteById(scheduleId);
	}

	@Override
	public List<LabCloseScheduleResponse> listByLabId(Long labId, LabCloseScheduleQueryCriteria criteria) {
		requireLab(labId);
		validateQueryCriteria(criteria);
		return labCloseScheduleRepository.listByLabId(labId, criteria)
				.stream()
				.map(LabManagementConverter::toCloseScheduleResponse)
				.toList();
	}

	private Lab requireLab(Long labId) {
		return labRepository.findById(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private Lab requireLabForUpdate(Long labId) {
		return labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private LabCloseSchedule requireCloseSchedule(Long labId, Long scheduleId) {
		return labCloseScheduleRepository.findByLabIdAndId(labId, scheduleId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_NOT_FOUND,
						"实验室临时关闭时间段不存在，labId=" + labId + ", scheduleId=" + scheduleId));
	}

	private LabCloseType parseCloseType(Integer closeType) {
		try {
			return LabCloseType.fromCode(closeType);
		}
		catch (IllegalArgumentException ex) {
			throw new LabManagementException(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID, "关闭类型只能是1到4");
		}
	}

	private void validateCloseTime(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID,
					"不开放结束时间必须晚于开始时间");
		}
	}

	private String normalizeReason(String reason) {
		if (!StringUtils.hasText(reason)) {
			return null;
		}
		String normalizedReason = reason.trim();
		if (normalizedReason.length() > 255) {
			throw new LabManagementException(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID,
					"不开放原因长度不能超过255");
		}
		return normalizedReason;
	}

	private void validateQueryCriteria(LabCloseScheduleQueryCriteria criteria) {
		if (criteria == null) {
			return;
		}
		if (criteria.getCloseType() != null) {
			parseCloseType(criteria.getCloseType());
		}
		if (criteria.getStartDate() != null && criteria.getEndDate() != null
				&& criteria.getEndDate().isBefore(criteria.getStartDate())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID,
					"查询结束日期不能早于开始日期");
		}
	}

	private List<LabReservation> listConflicts(Long labId, LocalDateTime closeStartTime, LocalDateTime closeEndTime) {
		LocalDateTime now = LocalDateTime.now();
		if (!closeEndTime.isAfter(now)) {
			return List.of();
		}
		return labReservationRepository.listPendingOrApprovedByLabIdAndDateRange(labId,
						closeStartTime.toLocalDate(), closeEndTime.toLocalDate())
				.stream()
				.filter(reservation -> reservation.getReservationDate().atTime(reservation.getEndTime()).isAfter(now))
				.filter(reservation -> isTimeOverlap(closeStartTime, closeEndTime,
						reservation.getReservationDate().atTime(reservation.getStartTime()),
						reservation.getReservationDate().atTime(reservation.getEndTime())))
				.toList();
	}

	private boolean isTimeOverlap(LocalDateTime leftStart, LocalDateTime leftEnd,
			LocalDateTime rightStart, LocalDateTime rightEnd) {
		return leftStart.isBefore(rightEnd) && leftEnd.isAfter(rightStart);
	}

	private void cancelReservations(Lab lab, List<LabReservation> reservations, String cancelReason) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		reservations.forEach(reservation -> reservation.cancelBySystem(cancelReason, now));
		labReservationRepository.updateBatch(reservations);
		reservations.forEach(reservation -> labOccupancyCacheManager.evict(reservation.getLabId(),
				reservation.getReservationDate()));
		publishReservationCancelledMessages(lab, reservations);
	}

	private String buildCancelReason(LabCloseType closeType, String reason) {
		String cancelReason = "实验室临时关闭安排调整，预约已自动取消；关闭类型：" + closeType.getDescription();
		if (StringUtils.hasText(reason)) {
			cancelReason += "；原因：" + reason;
		}
		return cancelReason;
	}

	private LabCloseScheduleMutationResponse buildMutationResponse(boolean applied, LabCloseSchedule closeSchedule,
			List<LabReservation> conflictedReservations) {
		return LabCloseScheduleMutationResponse.builder()
				.applied(applied)
				.closeSchedule(closeSchedule == null ? null : LabManagementConverter.toCloseScheduleResponse(closeSchedule))
				.conflictedReservations(toConflictedResponses(conflictedReservations))
				.build();
	}

	private List<ConflictedReservationResponse> toConflictedResponses(List<LabReservation> reservations) {
		if (reservations == null || reservations.isEmpty()) {
			return List.of();
		}
		return reservations.stream()
				.map(LabManagementConverter::toConflictedReservationResponse)
				.toList();
	}

	private void publishReservationCancelledMessages(Lab lab, List<LabReservation> reservations) {
		for (LabReservation reservation : reservations) {
			messagePublisher.publish(MessagePublishCommand.builder()
					.eventNo(MessageEventNoGenerator.generate(LabManagementConstants.RESERVATION_CANCELLED_MESSAGE_TYPE))
					.sourceSystem(LabManagementConstants.SOURCE_SYSTEM)
					.messageType(LabManagementConstants.RESERVATION_CANCELLED_MESSAGE_TYPE)
					.targetType(TargetType.SINGLE)
					.targetUserIds(List.of(reservation.getApplicantId()))
					.tempTemplate(TempMessageTemplate.builder()
							.titleTemplate("实验室预约已取消")
							.contentTemplate(buildReservationCancelledContent(lab, reservation))
							.contentFormat(ContentFormat.TEXT)
							.build())
					.occurredAt(LocalDateTime.now())
					.build());
		}
	}

	private String buildReservationCancelledContent(Lab lab, LabReservation reservation) {
		return "你预约的实验室因临时关闭安排，预约已自动取消。"
				+ "\n实验室：" + lab.getLabName()
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime()
				+ "\n取消原因：" + reservation.getCancelReason();
	}
}
