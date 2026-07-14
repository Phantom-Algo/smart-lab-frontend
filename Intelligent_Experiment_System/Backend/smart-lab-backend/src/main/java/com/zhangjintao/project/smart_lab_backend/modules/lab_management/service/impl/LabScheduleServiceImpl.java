package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleItemCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleSaveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ConflictedReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LabScheduleServiceImpl implements LabScheduleService {

	private static final String SCHEDULE_CHANGED_CANCEL_REASON = "实验室开放时间调整，预约不再处于开放时间内";

	private final LabRepository labRepository;
	private final LabScheduleRepository labScheduleRepository;
	private final LabReservationRepository labReservationRepository;
	private final LabScheduleCacheManager labScheduleCacheManager;
	private final LabOccupancyCacheManager labOccupancyCacheManager;
	private final MessagePublisher messagePublisher;

	@Override
	@Transactional
	public LabScheduleMutationResponse save(Long labId, LabScheduleSaveCommand command) {
		Lab lab = requireLabForUpdate(labId);
		Map<Integer, LabWeeklySchedule> requestedScheduleMap = normalizeSchedules(labId, command.getSchedules());
		List<LabReservation> conflictedReservations = listConflictsForScheduleUpdate(labId, requestedScheduleMap);
		if (!conflictedReservations.isEmpty() && !command.isForceCancelConflictedReservations()) {
			return buildMutationResponse(false, listSchedulesFromDatabase(labId), conflictedReservations);
		}

		labScheduleRepository.upsertBatch(requestedScheduleMap.values().stream().toList());
		cancelReservations(lab, conflictedReservations, SCHEDULE_CHANGED_CANCEL_REASON);
		evictScheduleCacheAfterCommit(labId);
		return buildMutationResponse(true, listSchedulesFromDatabase(labId), conflictedReservations);
	}

	@Override
	public List<LabScheduleResponse> listByLabId(Long labId) {
		requireLab(labId);
		return labScheduleCacheManager.listByLabId(labId, () -> labScheduleRepository.listByLabId(labId))
				.stream()
				.map(LabManagementConverter::toScheduleResponse)
				.toList();
	}

	@Override
	@Transactional
	public LabScheduleMutationResponse delete(Long labId, Long scheduleId, boolean forceCancelConflictedReservations) {
		Lab lab = requireLabForUpdate(labId);
		LabWeeklySchedule schedule = labScheduleRepository.findByLabIdAndId(labId, scheduleId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_SCHEDULE_NOT_FOUND,
						"实验室开放时间不存在，labId=" + labId + ", scheduleId=" + scheduleId));
		List<LabReservation> conflictedReservations = listConflictsForScheduleDelete(labId, schedule.getDayOfWeek());
		if (!conflictedReservations.isEmpty() && !forceCancelConflictedReservations) {
			return buildMutationResponse(false, listSchedulesFromDatabase(labId), conflictedReservations);
		}

		labScheduleRepository.deleteById(scheduleId);
		cancelReservations(lab, conflictedReservations, SCHEDULE_CHANGED_CANCEL_REASON);
		evictScheduleCacheAfterCommit(labId);
		return buildMutationResponse(true, listSchedulesFromDatabase(labId), conflictedReservations);
	}

	private Lab requireLab(Long labId) {
		return labRepository.findById(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private Lab requireLabForUpdate(Long labId) {
		return labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private Map<Integer, LabWeeklySchedule> normalizeSchedules(Long labId, List<LabScheduleItemCommand> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SCHEDULE_PARAM_INVALID, "开放时间数组不能为空");
		}
		Map<Integer, LabWeeklySchedule> scheduleMap = new LinkedHashMap<>();
		for (LabScheduleItemCommand item : schedules) {
			validateScheduleItem(item);
			if (scheduleMap.containsKey(item.getDayOfWeek())) {
				throw new LabManagementException(LabManagementErrorCode.LAB_SCHEDULE_PARAM_INVALID,
						"同一次请求中不能重复设置同一星期，dayOfWeek=" + item.getDayOfWeek());
			}
			scheduleMap.put(item.getDayOfWeek(), LabWeeklySchedule.builder()
					.labId(labId)
					.dayOfWeek(item.getDayOfWeek())
					.openTime(item.getOpenTime())
					.closeTime(item.getCloseTime())
					.build());
		}
		return scheduleMap;
	}

	private void validateScheduleItem(LabScheduleItemCommand item) {
		if (item.getDayOfWeek() == null || item.getDayOfWeek() < 1 || item.getDayOfWeek() > 7) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SCHEDULE_PARAM_INVALID, "星期只能是1到7");
		}
		if (item.getOpenTime() == null || item.getCloseTime() == null || !item.getCloseTime().isAfter(item.getOpenTime())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SCHEDULE_PARAM_INVALID,
					"开放时间结束必须晚于开始时间");
		}
	}

	private List<LabReservation> listConflictsForScheduleUpdate(Long labId,
			Map<Integer, LabWeeklySchedule> requestedScheduleMap) {
		LocalDateTime now = LocalDateTime.now();
		return labReservationRepository.listFuturePendingOrApprovedByLabId(labId, now.toLocalDate())
				.stream()
				.filter(reservation -> reservation.getReservationDate().atTime(reservation.getStartTime()).isAfter(now))
				.filter(reservation -> requestedScheduleMap.containsKey(reservation.getReservationDate().getDayOfWeek().getValue()))
				.filter(reservation -> !isWithinSchedule(reservation,
						requestedScheduleMap.get(reservation.getReservationDate().getDayOfWeek().getValue())))
				.toList();
	}

	private List<LabReservation> listConflictsForScheduleDelete(Long labId, Integer dayOfWeek) {
		LocalDateTime now = LocalDateTime.now();
		return labReservationRepository.listFuturePendingOrApprovedByLabId(labId, now.toLocalDate())
				.stream()
				.filter(reservation -> reservation.getReservationDate().atTime(reservation.getStartTime()).isAfter(now))
				.filter(reservation -> reservation.getReservationDate().getDayOfWeek().getValue() == dayOfWeek)
				.toList();
	}

	private boolean isWithinSchedule(LabReservation reservation, LabWeeklySchedule schedule) {
		return !reservation.getStartTime().isBefore(schedule.getOpenTime())
				&& !reservation.getEndTime().isAfter(schedule.getCloseTime());
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

	private LabScheduleMutationResponse buildMutationResponse(boolean applied, List<LabWeeklySchedule> schedules,
			List<LabReservation> conflictedReservations) {
		return LabScheduleMutationResponse.builder()
				.applied(applied)
				.schedules(schedules.stream().map(LabManagementConverter::toScheduleResponse).toList())
				.conflictedReservations(toConflictedResponses(conflictedReservations))
				.build();
	}

	private List<LabWeeklySchedule> listSchedulesFromDatabase(Long labId) {
		return labScheduleRepository.listByLabId(labId);
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
		return "你预约的实验室因开放时间调整，预约已自动取消。"
				+ "\n实验室：" + lab.getLabName()
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime()
				+ "\n取消原因：" + reservation.getCancelReason();
	}

	private void evictScheduleCacheAfterCommit(Long labId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			labScheduleCacheManager.evict(labId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				labScheduleCacheManager.evict(labId);
			}
		});
	}
}
