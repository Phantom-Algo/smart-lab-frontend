package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabCloseScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabStatusChangeLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabStatusChangeLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationApproveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCancelCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabStatusTransitionPolicy;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.ReservationService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.utils.ReservationNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

	private static final String MANUAL_STATUS_CHANGE_SOURCE = "MANUAL";

	private final LabRepository labRepository;
	private final LabReservationRepository labReservationRepository;
	private final LabScheduleRepository labScheduleRepository;
	private final LabCloseScheduleRepository labCloseScheduleRepository;
	private final LabStatusChangeLogRepository labStatusChangeLogRepository;
	private final PersonDirectoryPort personDirectoryPort;
	private final MessagePublisher messagePublisher;
	private final RealtimePublisher realtimePublisher;
	private final LabOccupancyCacheManager labOccupancyCacheManager;
	private final LabScheduleCacheManager labScheduleCacheManager;

	@Override
	@Transactional
	public ReservationResponse create(ReservationCreateCommand command) {
		validateCreateCommand(command);
		Lab lab = requireLabForUpdate(command.getLabId());
		LabStatusTransitionPolicy.validateReservable(lab);
		validateOpenSchedule(command);
		validateCloseSchedule(command);
		validateReservationConflict(command);

		LabReservation reservation = LabReservation.builder()
				.labId(command.getLabId())
				.applicantId(command.getApplicantId())
				.reservationDate(command.getReservationDate())
				.startTime(command.getStartTime())
				.endTime(command.getEndTime())
				.purpose(command.getPurpose())
				.courseName(command.getCourseName())
				.reservationStatus(LabReservationStatus.PENDING_APPROVAL)
				.build();
		LabReservation savedReservation = saveWithUniqueReservationNo(reservation);
		evictOccupancy(savedReservation);
		publishReservationRealtime("reservation.created", savedReservation, Map.of());
		publishPendingReviewMessage(savedReservation);
		return buildResponse(savedReservation);
	}

	@Override
	@Transactional
	public ReservationResponse approve(ReservationApproveCommand command) {
		if (command.getApproved() == null) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID, "审核结果不能为空");
		}
		LabReservation reservation = requireReservationForUpdate(command.getReservationNo());
		if (reservation.getReservationStatus() != LabReservationStatus.PENDING_APPROVAL) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID);
		}

		LocalDateTime now = LocalDateTime.now();
		if (Boolean.TRUE.equals(command.getApproved())) {
			reservation.approve(command.getApproverId(), command.getApproveComment(), now);
		}
		else {
			reservation.reject(command.getApproverId(), command.getApproveComment(), now);
		}
		LabReservation updatedReservation = labReservationRepository.update(reservation);
		evictOccupancy(updatedReservation);
		publishApprovalMessage(updatedReservation);
		publishReservationRealtime(updatedReservation.getReservationStatus() == LabReservationStatus.APPROVED
				? "reservation.approved" : "reservation.rejected",
				updatedReservation,
				StringUtils.hasText(updatedReservation.getApproveComment())
						? Map.of("approveComment", updatedReservation.getApproveComment())
						: Map.of());
		return buildResponse(updatedReservation);
	}

	@Override
	@Transactional
	public ReservationResponse cancel(ReservationCancelCommand command) {
		LabReservation reservation = requireReservationForUpdate(command.getReservationNo());
		if (!reservation.belongsToApplicant(command.getApplicantId())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_PERMISSION_DENIED);
		}
		if (!LabReservationStatus.CANCELLABLE_BY_APPLICANT.contains(reservation.getReservationStatus().getCode())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID);
		}
		if (!StringUtils.hasText(command.getCancelReason())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID, "取消原因不能为空");
		}
		reservation.cancelByApplicant(command.getCancelReason(), LocalDateTime.now());
		LabReservation updatedReservation = labReservationRepository.update(reservation);
		evictOccupancy(updatedReservation);
		publishReservationRealtime("reservation.cancelled", updatedReservation,
				Map.of("cancelReason", updatedReservation.getCancelReason()));
		return buildResponse(updatedReservation);
	}

	@Override
	@Transactional
	public ReservationResponse checkIn(String reservationNo, Long applicantId) {
		return checkInInternal(reservationNo, applicantId, true);
	}

	@Override
	@Transactional
	public ReservationResponse adminCheckIn(String reservationNo, Long operatorId) {
		return checkInInternal(reservationNo, operatorId, false);
	}

	private ReservationResponse checkInInternal(String reservationNo, Long operatorId, boolean requireApplicant) {
		LabReservation snapshot = requireReservation(reservationNo);
		Lab lab = requireLabForUpdate(snapshot.getLabId());
		LabRunningStatus fromRunningStatus = lab.getRunningStatus();
		LabOpenStatus fromOpenStatus = lab.getOpenStatus();
		LabReservation reservation = requireReservationForUpdate(reservationNo);
		if (requireApplicant) {
			validateApplicant(reservation, operatorId);
		}
		if (reservation.getReservationStatus() != LabReservationStatus.APPROVED) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID);
		}
		LocalDateTime now = LocalDateTime.now();
		validateCheckInWindow(reservation, now);

		LabStatusTransitionPolicy.applyCheckIn(lab);
		reservation.checkIn(now);
		labRepository.updateStatus(lab);
		LabReservation updatedReservation = labReservationRepository.update(reservation);
		if (!requireApplicant) {
			writeAdminCheckInStatusLog(lab, reservationNo, operatorId, fromRunningStatus, fromOpenStatus, now);
		}
		evictOccupancy(updatedReservation);
		publishReservationRealtime("reservation.checked_in", updatedReservation,
				Map.of("operatorId", operatorId));
		publishLabStatusRealtime(lab, fromRunningStatus, fromOpenStatus, "预约签到");
		return buildResponse(updatedReservation);
	}

	@Override
	@Transactional
	public ReservationResponse checkOut(String reservationNo, Long applicantId) {
		LabReservation snapshot = requireReservation(reservationNo);
		Lab lab = requireLabForUpdate(snapshot.getLabId());
		LabRunningStatus fromRunningStatus = lab.getRunningStatus();
		LabOpenStatus fromOpenStatus = lab.getOpenStatus();
		LabReservation reservation = requireReservationForUpdate(reservationNo);
		validateApplicant(reservation, applicantId);
		if (reservation.getReservationStatus() != LabReservationStatus.CHECKED_IN
				|| reservation.getCheckInAt() == null
				|| reservation.getCheckOutAt() != null) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_STATUS_INVALID);
		}
		LocalDateTime now = LocalDateTime.now();
		validateCheckOutTime(reservation, now);

		LabStatusTransitionPolicy.applyCheckOut(lab);
		reservation.checkOut(now);
		labRepository.updateStatus(lab);
		LabReservation updatedReservation = labReservationRepository.update(reservation);
		evictOccupancy(updatedReservation);
		publishReservationRealtime("reservation.checked_out", updatedReservation,
				Map.of("operatorId", applicantId));
		publishLabStatusRealtime(lab, fromRunningStatus, fromOpenStatus, "预约签退");
		return buildResponse(updatedReservation);
	}

	@Override
	public PageResult<ReservationResponse> pageMine(MyReservationQueryCriteria criteria) {
		PageResult<LabReservation> pageResult = labReservationRepository.pageMine(criteria);
		return PageResult.of(buildResponses(pageResult.getRecords()), pageResult.getTotal(),
				pageResult.getPageNum(), pageResult.getPageSize());
	}

	@Override
	public PageResult<ReservationResponse> pageAll(MyReservationQueryCriteria criteria) {
		PageResult<LabReservation> pageResult = labReservationRepository.pageAll(criteria);
		return PageResult.of(buildResponses(pageResult.getRecords()), pageResult.getTotal(),
				pageResult.getPageNum(), pageResult.getPageSize());
	}

	@Override
	public PageResult<ReservationResponse> pagePending(PendingReservationQueryCriteria criteria) {
		PendingReservationQueryCriteria safeCriteria = normalizePendingCriteria(criteria);
		PageResult<LabReservation> pageResult = labReservationRepository.pagePending(safeCriteria);
		return PageResult.of(buildResponses(pageResult.getRecords()), pageResult.getTotal(),
				pageResult.getPageNum(), pageResult.getPageSize());
	}

	@Override
	public LabOccupancyResponse getOccupancy(Long labId, LocalDate date) {
		requireLab(labId);
		List<LabOccupancyResponse.OccupiedTime> occupiedTimes = labOccupancyCacheManager
				.listOccupiedTimes(labId, date, () -> labReservationRepository.listOccupancy(labId, date).stream()
						.map(LabManagementConverter::toOccupiedTime)
						.toList());
		return LabOccupancyResponse.builder()
				.labId(labId)
				.date(date)
				.occupiedTimes(occupiedTimes)
				.build();
	}

	@Override
	@Transactional
	public void markAbsentReservations() {
		LocalDateTime now = LocalDateTime.now();
		List<LabReservation> absentReservations = labReservationRepository.listApprovedUncheckedByDate(now.toLocalDate())
				.stream()
				.filter(reservation -> shouldMarkAbsent(reservation, now))
				.peek(reservation -> reservation.markAbsent(now))
				.toList();
		if (absentReservations.isEmpty()) {
			return;
		}
		labReservationRepository.updateBatch(absentReservations);
		absentReservations.forEach(this::evictOccupancy);
		absentReservations.forEach(this::publishAbsentMessage);
		absentReservations.forEach(reservation -> publishReservationRealtime("reservation.absent", reservation, Map.of()));
	}

	private void validateCreateCommand(ReservationCreateCommand command) {
		if (command.getApplicantId() == null) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_PERMISSION_DENIED, "申请人不能为空");
		}
		if (command.getEndTime() == null || command.getStartTime() == null
				|| !command.getEndTime().isAfter(command.getStartTime())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_TIME_INVALID,
					"预约结束时间必须晚于开始时间");
		}
		LocalDateTime startDateTime = command.getReservationDate().atTime(command.getStartTime());
		if (!startDateTime.isAfter(LocalDateTime.now())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_TIME_INVALID,
					"预约开始时间必须晚于当前时间");
		}
	}

	private void validateOpenSchedule(ReservationCreateCommand command) {
		int dayOfWeek = command.getReservationDate().getDayOfWeek().getValue();
		List<LabWeeklySchedule> schedules = labScheduleCacheManager
				.listByLabId(command.getLabId(), () -> labScheduleRepository.listByLabId(command.getLabId()));

		LocalTime openTime = LocalTime.of(8, 0);
		LocalTime closeTime = LocalTime.of(22, 0);
		if (schedules != null && !schedules.isEmpty()) {
			LabWeeklySchedule schedule = schedules.stream()
					.filter(item -> item.getDayOfWeek().equals(dayOfWeek))
					.findFirst()
					.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.RESERVATION_OUT_OF_OPEN_SCHEDULE));
			openTime = schedule.getOpenTime();
			closeTime = schedule.getCloseTime();
		}

		if (command.getStartTime().isBefore(openTime) || command.getEndTime().isAfter(closeTime)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_OUT_OF_OPEN_SCHEDULE);
		}
	}

	private void validateCloseSchedule(ReservationCreateCommand command) {
		LocalDateTime startDateTime = command.getReservationDate().atTime(command.getStartTime());
		LocalDateTime endDateTime = command.getReservationDate().atTime(command.getEndTime());
		if (labCloseScheduleRepository.existsOverlap(command.getLabId(), startDateTime, endDateTime)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_CLOSE_SCHEDULE_CONFLICT);
		}
	}

	private void validateReservationConflict(ReservationCreateCommand command) {
		if (labReservationRepository.existsActiveOverlap(command.getLabId(), command.getReservationDate(),
				command.getStartTime(), command.getEndTime())) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_TIME_CONFLICT);
		}
	}

	private LabReservation saveWithUniqueReservationNo(LabReservation reservation) {
		for (int i = 0; i < LabManagementConstants.RESERVATION_NO_MAX_RETRY; i++) {
			String reservationNo = ReservationNoGenerator.generate(reservation.getReservationDate());
			if (labReservationRepository.existsByReservationNo(reservationNo)) {
				continue;
			}
			reservation.setReservationNo(reservationNo);
			try {
				return labReservationRepository.save(reservation);
			}
			catch (DuplicateKeyException ex) {
				if (i == LabManagementConstants.RESERVATION_NO_MAX_RETRY - 1) {
					throw new LabManagementException(LabManagementErrorCode.RESERVATION_NO_GENERATE_FAILED);
				}
			}
		}
		throw new LabManagementException(LabManagementErrorCode.RESERVATION_NO_GENERATE_FAILED);
	}

	private void validateApplicant(LabReservation reservation, Long applicantId) {
		if (!reservation.belongsToApplicant(applicantId)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_PERMISSION_DENIED);
		}
	}

	private void validateCheckInWindow(LabReservation reservation, LocalDateTime now) {
		LocalDateTime startDateTime = reservation.getReservationDate().atTime(reservation.getStartTime());
		LocalDateTime endDateTime = reservation.getReservationDate().atTime(reservation.getEndTime());
		LocalDateTime earliestCheckIn = startDateTime.minusMinutes(
				LabManagementConstants.RESERVATION_CHECK_IN_BEFORE_START_MINUTES);
		LocalDateTime latestCheckIn = startDateTime.plusMinutes(
				LabManagementConstants.RESERVATION_CHECK_IN_AFTER_START_MINUTES);
		if (latestCheckIn.isAfter(endDateTime)) {
			latestCheckIn = endDateTime;
		}
		if (now.isBefore(earliestCheckIn) || now.isAfter(latestCheckIn)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_CHECK_IN_WINDOW_INVALID);
		}
	}

	private void validateCheckOutTime(LabReservation reservation, LocalDateTime now) {
		LocalDateTime endDateTime = reservation.getReservationDate().atTime(reservation.getEndTime());
		if (!now.isAfter(reservation.getCheckInAt()) || now.isBefore(endDateTime)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_CHECK_OUT_TIME_INVALID);
		}
	}

	private PendingReservationQueryCriteria normalizePendingCriteria(PendingReservationQueryCriteria criteria) {
		LocalDate today = LocalDate.now();
		LocalDate requestedStartDate = criteria.getStartDate();
		LocalDate startDate = requestedStartDate == null || requestedStartDate.isBefore(today)
				? today : requestedStartDate;
		LocalDate endDate = criteria.getEndDate();
		if (endDate != null && endDate.isBefore(startDate)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_TIME_INVALID,
					"查询结束日期不能早于开始日期");
		}
		return PendingReservationQueryCriteria.builder()
				.labId(criteria.getLabId())
				.startDate(startDate)
				.endDate(endDate)
				.page(criteria.getPage())
				.pageSize(criteria.getPageSize())
				.build();
	}

	private boolean shouldMarkAbsent(LabReservation reservation, LocalDateTime now) {
		LocalDateTime startDateTime = reservation.getReservationDate().atTime(reservation.getStartTime());
		LocalDateTime endDateTime = reservation.getReservationDate().atTime(reservation.getEndTime());
		return !now.isBefore(startDateTime.plusMinutes(LabManagementConstants.RESERVATION_CHECK_IN_AFTER_START_MINUTES))
				|| !now.isBefore(endDateTime);
	}

	private Lab requireLab(Long labId) {
		return labRepository.findById(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private Lab requireLabForUpdate(Long labId) {
		return labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private LabReservation requireReservation(String reservationNo) {
		return labReservationRepository.findByReservationNo(reservationNo)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.RESERVATION_NOT_FOUND));
	}

	private LabReservation requireReservationForUpdate(String reservationNo) {
		return labReservationRepository.findByReservationNoForUpdate(reservationNo)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.RESERVATION_NOT_FOUND));
	}

	private ReservationResponse buildResponse(LabReservation reservation) {
		return buildResponses(List.of(reservation)).getFirst();
	}

	private List<ReservationResponse> buildResponses(List<LabReservation> reservations) {
		if (reservations == null || reservations.isEmpty()) {
			return List.of();
		}
		Map<Long, Lab> labMap = labRepository.mapByIdsIncludingDeleted(reservations.stream()
				.map(LabReservation::getLabId)
				.toList());
		Map<Long, String> userNameMap = mapUserNames(reservations);
		return reservations.stream()
				.map(reservation -> LabManagementConverter.toReservationResponse(reservation,
						requireLabName(reservation.getLabId(), labMap),
						requireUserName(reservation.getApplicantId(), userNameMap),
						reservation.getApproverId() == null ? null : requireUserName(reservation.getApproverId(), userNameMap)))
				.toList();
	}

	private String requireLabName(Long labId, Map<Long, Lab> labMap) {
		Lab lab = labMap.get(labId);
		if (lab == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND, "预约关联实验室不存在，labId=" + labId);
		}
		return lab.getLabName();
	}

	private Map<Long, String> mapUserNames(List<LabReservation> reservations) {
		return reservations.stream()
				.flatMap(reservation -> Stream.of(reservation.getApplicantId(), reservation.getApproverId()))
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toMap(Function.identity(), this::resolveUserName));
	}

	private String requireUserName(Long userId, Map<Long, String> userNameMap) {
		String userName = userNameMap.get(userId);
		if (!StringUtils.hasText(userName)) {
			throw new LabManagementException(LabManagementErrorCode.RESERVATION_RELATED_USER_NOT_FOUND,
					"预约关联用户不存在，userId=" + userId);
		}
		return userName;
	}

	private String resolveUserName(Long userId) {
		return personDirectoryPort.getByUserId(userId)
				.map(PersonDirectoryItemDTO::getPersonName)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.RESERVATION_RELATED_USER_NOT_FOUND,
						"预约关联用户不存在，userId=" + userId));
	}

	private void evictOccupancy(LabReservation reservation) {
		labOccupancyCacheManager.evict(reservation.getLabId(), reservation.getReservationDate());
	}

	private void writeAdminCheckInStatusLog(Lab lab, String reservationNo, Long operatorId,
			LabRunningStatus fromRunningStatus, LabOpenStatus fromOpenStatus, LocalDateTime now) {
		labStatusChangeLogRepository.save(LabStatusChangeLog.builder()
				.labId(lab.getId())
				.operatorId(operatorId)
				.fromRunningStatus(fromRunningStatus)
				.toRunningStatus(lab.getRunningStatus())
				.fromOpenStatus(fromOpenStatus)
				.toOpenStatus(lab.getOpenStatus())
				.changeReason("管理员帮签预约：" + reservationNo)
				.triggerSource(MANUAL_STATUS_CHANGE_SOURCE)
				.forcedCheckoutCount(0)
				.cancelledReservationCount(0)
				.createdAt(now)
				.build());
	}

	private void publishApprovalMessage(LabReservation reservation) {
		if (reservation.getReservationStatus() == LabReservationStatus.APPROVED) {
			publishSingleReservationMessage(reservation, LabManagementConstants.RESERVATION_APPROVED_MESSAGE_TYPE,
					"实验室预约已通过", buildApprovedContent(reservation));
			return;
		}
		if (reservation.getReservationStatus() == LabReservationStatus.REJECTED) {
			publishSingleReservationMessage(reservation, LabManagementConstants.RESERVATION_REJECTED_MESSAGE_TYPE,
					"实验室预约已驳回", buildRejectedContent(reservation));
		}
	}

	private void publishPendingReviewMessage(LabReservation reservation) {
		String labName = requireLabName(reservation.getLabId(),
				labRepository.mapByIdsIncludingDeleted(List.of(reservation.getLabId())));
		String applicantName = resolveUserName(reservation.getApplicantId());
		String title = "新的实验室预约待审核";
		String content = "有新的实验室预约申请需要处理。"
				+ "\n申请人：" + applicantName
				+ "\n实验室：" + labName
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime();
		try {
			messagePublisher.publish(MessagePublishCommand.builder()
					.eventNo(MessageEventNoGenerator.generate(LabManagementConstants.RESERVATION_PENDING_REVIEW_MESSAGE_TYPE))
					.sourceSystem(LabManagementConstants.SOURCE_SYSTEM)
					.messageType(LabManagementConstants.RESERVATION_PENDING_REVIEW_MESSAGE_TYPE)
					.targetType(TargetType.ALL_MANAGER)
					.tempTemplate(TempMessageTemplate.builder()
							.titleTemplate(title)
							.contentTemplate(content)
							.contentFormat(ContentFormat.TEXT)
							.build())
					.occurredAt(LocalDateTime.now())
					.build());
		}
		catch (Exception ex) {
			log.warn("Failed to publish reservation pending review message, reservationNo={}",
					reservation.getReservationNo(), ex);
		}
	}

	private void publishAbsentMessage(LabReservation reservation) {
		publishSingleReservationMessage(reservation, LabManagementConstants.RESERVATION_ABSENT_MESSAGE_TYPE,
				"实验室预约已标记爽约", buildAbsentContent(reservation));
	}

	private void publishSingleReservationMessage(LabReservation reservation, String messageType, String title,
			String content) {
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo(MessageEventNoGenerator.generate(messageType))
				.sourceSystem(LabManagementConstants.SOURCE_SYSTEM)
				.messageType(messageType)
				.targetType(TargetType.SINGLE)
				.targetUserIds(List.of(reservation.getApplicantId()))
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate(title)
						.contentTemplate(content)
						.contentFormat(ContentFormat.TEXT)
						.build())
				.occurredAt(LocalDateTime.now())
				.build());
	}

	private String buildApprovedContent(LabReservation reservation) {
		return buildBaseReservationContent("你的实验室预约已审核通过。", reservation);
	}

	private String buildRejectedContent(LabReservation reservation) {
		String content = buildBaseReservationContent("你的实验室预约已被驳回。", reservation);
		if (StringUtils.hasText(reservation.getApproveComment())) {
			content += "\n审核意见：" + reservation.getApproveComment();
		}
		return content;
	}

	private String buildAbsentContent(LabReservation reservation) {
		return buildBaseReservationContent("你未在规定时间内签到，预约已被系统标记为爽约。", reservation);
	}

	private String buildBaseReservationContent(String prefix, LabReservation reservation) {
		String labName = requireLabName(reservation.getLabId(),
				labRepository.mapByIdsIncludingDeleted(List.of(reservation.getLabId())));
		return prefix
				+ "\n实验室：" + labName
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime();
	}

	private void publishReservationRealtime(String eventName, LabReservation reservation, Map<String, Object> extraData) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationNo", reservation.getReservationNo());
		payload.put("labId", reservation.getLabId());
		payload.put("applicantId", reservation.getApplicantId());
		payload.put("reservationDate", reservation.getReservationDate());
		payload.put("startTime", reservation.getStartTime());
		payload.put("endTime", reservation.getEndTime());
		payload.put("status", reservation.getReservationStatus().getCode());
		payload.put("statusDesc", reservation.getReservationStatus().getDescription());
		payload.putAll(extraData);
		realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
				.event(eventName)
				.source(LabManagementConstants.SOURCE_SYSTEM)
				.targetType(RealtimeTargetType.BROADCAST)
				.data(payload)
				.occurredAt(LocalDateTime.now())
				.build());
	}

	private void publishLabStatusRealtime(Lab lab, LabRunningStatus fromRunningStatus,
			LabOpenStatus fromOpenStatus, String reason) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("labId", lab.getId());
		payload.put("runningStatus", lab.getRunningStatus().getCode());
		payload.put("runningStatusDesc", lab.getRunningStatus().getDescription());
		payload.put("openStatus", lab.getOpenStatus().getCode());
		payload.put("openStatusDesc", lab.getOpenStatus().getDescription());
		payload.put("fromRunningStatus", fromRunningStatus == null ? null : fromRunningStatus.getCode());
		payload.put("fromOpenStatus", fromOpenStatus == null ? null : fromOpenStatus.getCode());
		payload.put("reason", reason);
		realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
				.event("lab.status_changed")
				.source(LabManagementConstants.SOURCE_SYSTEM)
				.targetType(RealtimeTargetType.BROADCAST)
				.data(payload)
				.occurredAt(LocalDateTime.now())
				.build());
	}
}
