package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabStatusChangeLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabStatusChangeLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ConflictedReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabStatusUpdateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabStatusTransitionPolicy;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

	private static final String LAB_DELETED_CANCEL_REASON = "实验室已被删除，关联预约自动取消";
	private static final String MANUAL_STATUS_CHANGE_SOURCE = "MANUAL";

	private final LabRepository labRepository;
	private final LabTypeRepository labTypeRepository;
	private final LabScheduleRepository labScheduleRepository;
	private final LabReservationRepository labReservationRepository;
	private final LabStatusChangeLogRepository labStatusChangeLogRepository;
	private final LabScheduleCacheManager labScheduleCacheManager;
	private final LabOccupancyCacheManager labOccupancyCacheManager;
	private final LabSensorBindingService labSensorBindingService;
	private final PersonDirectoryPort personDirectoryPort;
	private final MessagePublisher messagePublisher;
	private final RealtimePublisher realtimePublisher;

	@Override
	@Transactional
	public LabDetailResponse create(LabCreateCommand command) {
		Lab lab = Lab.create(command);
		validateLab(lab);
		requireLabType(lab.getLabTypeId());
		validateDutyUser(lab.getDutyUserId());
		validateLocationUnique(lab.getLocation(), null);
		Lab savedLab = labRepository.save(lab);
		publishLabInfoRealtime("lab.created", savedLab);
		return buildDetail(savedLab);
	}

	@Override
	@Transactional
	public LabDetailResponse update(Long labId, LabUpdateCommand command) {
		Lab lab = requireLab(labId);
		LabUpdateCommand mergedCommand = mergeUpdateCommand(lab, command);
		lab.applyUpdate(mergedCommand);
		validateLab(lab);
		requireLabType(lab.getLabTypeId());
		validateDutyUser(lab.getDutyUserId());
		validateLocationUnique(lab.getLocation(), lab.getId());
		Lab updatedLab = labRepository.update(lab);
		publishLabInfoRealtime("lab.info_updated", updatedLab);
		return buildDetail(updatedLab);
	}

	@Override
	@Transactional
	public LabStatusUpdateResponse updateStatus(Long labId, LabStatusUpdateCommand command) {
		validateStatusUpdateCommand(command);
		Lab lab = requireLabForUpdate(labId);
		LabRunningStatus fromRunning = lab.getRunningStatus();
		LabOpenStatus fromOpen = lab.getOpenStatus();
		ResolvedLabStatus target = resolveTargetStatus(lab, command);
		LabStatusTransitionPolicy.validateManualTransition(fromRunning, fromOpen,
				target.runningStatus(), target.openStatus(), command.getRunningStatus() != null);

		if (fromRunning == target.runningStatus() && fromOpen == target.openStatus()) {
			return buildStatusUpdateResponse(true, buildDetail(lab), List.of(), List.of(), List.of(), "实验室状态未发生变化");
		}

		List<LabReservation> activeReservations = listActiveReservationsIfNeeded(lab, target);
		if (!activeReservations.isEmpty() && !command.isForceFinishCurrentUsage()) {
			return buildStatusUpdateResponse(false, buildDetail(lab), activeReservations, List.of(), List.of(),
					"当前实验室存在正在使用的预约，请确认是否强制结束使用");
		}

		List<LabReservation> futureReservations = listFutureReservationsIfNeeded(lab, target);
		if (!futureReservations.isEmpty() && !command.isForceCancelFutureReservations()) {
			return buildStatusUpdateResponse(false, buildDetail(lab), futureReservations, List.of(), List.of(),
					"目标状态会导致未来预约不可执行，请确认是否自动取消关联预约");
		}

		LocalDateTime now = LocalDateTime.now();
		forceCheckOutReservations(activeReservations, now);
		cancelFutureReservations(lab, futureReservations, buildStatusChangeCancelReason(target), now);
		lab.setRunningStatus(target.runningStatus());
		lab.setOpenStatus(target.openStatus());
		labRepository.updateStatus(lab);
		writeStatusChangeLog(lab, command, fromRunning, fromOpen, target, activeReservations.size(),
				futureReservations.size(), now);
		publishForcedCheckoutMessages(lab, activeReservations, command.getChangeReason());
		publishReservationCancelledMessages(lab, futureReservations);
		publishLabStatusChangedMessage(lab, fromRunning, fromOpen, target, command.getChangeReason());
		publishLabStatusRealtime(lab, fromRunning, fromOpen, target, command.getChangeReason(),
				activeReservations.size(), futureReservations.size());

		return buildStatusUpdateResponse(true, buildDetail(lab), List.of(), futureReservations, activeReservations, null);
	}

	@Override
	@Transactional
	public void delete(Long labId) {
		Lab lab = requireLabForUpdate(labId);
		if (labSensorBindingService.existsActiveBindingByLabId(labId)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_HAS_ACTIVE_SENSOR_BINDING,
					"实验室存在有效传感器绑定，请先换绑或删除传感器，labId=" + labId);
		}
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();
		List<LabReservation> reservations = labReservationRepository.listFuturePendingOrApprovedByLabId(labId, today);
		reservations.forEach(reservation -> reservation.cancelByLabDeleted(LAB_DELETED_CANCEL_REASON, now));
		labReservationRepository.updateBatch(reservations);
		lab.markDeleted(now);
		labRepository.softDelete(lab);
		publishReservationCancelledMessages(lab, reservations);
		publishLabInfoRealtime("lab.deleted", lab);
	}

	@Override
	public PageResult<LabResponse> page(LabQueryCriteria criteria) {
		PageResult<LabPageItemDTO> pageResult = labRepository.page(criteria);
		Map<Long, LabType> labTypeMap = labTypeRepository.mapByIds(pageResult.getRecords().stream()
				.map(LabPageItemDTO::getLabTypeId)
				.toList());
		Map<Long, String> dutyUserNameMap = mapDutyUserNames(pageResult.getRecords().stream()
				.map(LabPageItemDTO::getDutyUserId)
				.toList());
		List<LabResponse> records = pageResult.getRecords().stream()
				.map(item -> LabManagementConverter.toLabResponse(item,
						requireLabTypeName(item.getLabTypeId(), labTypeMap),
						item.getDutyUserId() == null ? null : dutyUserNameMap.get(item.getDutyUserId())))
				.toList();
		return PageResult.of(records, pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
	}

	@Override
	public LabDetailResponse getById(Long labId) {
		return buildDetail(requireLab(labId));
	}

	private Lab requireLab(Long labId) {
		return labRepository.findById(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private Lab requireLabForUpdate(Long labId) {
		return labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
	}

	private LabType requireLabType(Long labTypeId) {
		return labTypeRepository.findById(labTypeId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_TYPE_NOT_FOUND));
	}

	private String requireLabTypeName(Long labTypeId, Map<Long, LabType> labTypeMap) {
		LabType labType = labTypeMap.get(labTypeId);
		if (labType == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_TYPE_NOT_FOUND,
					"实验室类型不存在，labTypeId=" + labTypeId);
		}
		return labType.getLabTypeName();
	}

	private void validateLab(Lab lab) {
		if (!StringUtils.hasText(lab.getLabName())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "实验室名称不能为空");
		}
		if (lab.getLabTypeId() == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "实验室类型不能为空");
		}
		if (lab.getCapacity() == null || lab.getCapacity() < 0) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "最大容纳人数不能小于0");
		}
		if (lab.getAreaM2() != null && lab.getAreaM2().compareTo(BigDecimal.ZERO) <= 0) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "面积必须大于0");
		}
		Integer floor = lab.getLocation() == null ? null : lab.getLocation().getFloor();
		if (floor != null && (floor < 1 || floor > 99)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "楼层范围必须是1到99");
		}
	}

	private void validateDutyUser(Long dutyUserId) {
		if (dutyUserId == null) {
			return;
		}
		personDirectoryPort.getByUserId(dutyUserId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.DUTY_USER_NOT_FOUND,
						"负责人用户不存在，dutyUserId=" + dutyUserId));
	}

	private void validateLocationUnique(LabLocation location, Long excludeId) {
		if (location == null || !location.isCompleteForUniqueCheck()) {
			return;
		}
		if (labRepository.existsByLocation(location, excludeId)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_LOCATION_DUPLICATE);
		}
	}

	private void validateStatusUpdateCommand(LabStatusUpdateCommand command) {
		if (command == null || (command.getRunningStatus() == null && command.getOpenStatus() == null)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "至少需要传入一个目标状态");
		}
		if (!StringUtils.hasText(command.getChangeReason())) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "状态变更原因不能为空");
		}
		String reason = command.getChangeReason().trim();
		if (reason.length() > 255) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "状态变更原因长度不能超过255");
		}
		parseRunningStatus(command.getRunningStatus());
		parseOpenStatus(command.getOpenStatus());
	}

	private ResolvedLabStatus resolveTargetStatus(Lab lab, LabStatusUpdateCommand command) {
		boolean runningRequested = command.getRunningStatus() != null;
		boolean openRequested = command.getOpenStatus() != null;
		LabRunningStatus targetRunning = runningRequested
				? parseRunningStatus(command.getRunningStatus()) : lab.getRunningStatus();
		LabOpenStatus targetOpen = openRequested
				? parseOpenStatus(command.getOpenStatus()) : lab.getOpenStatus();

		if (targetOpen == LabOpenStatus.PERMANENT_CLOSED) {
			if (!command.isConfirmPermanentClose()) {
				throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
						"永久关闭实验室需要显式确认");
			}
			targetRunning = LabRunningStatus.DISABLED;
		}
		if (targetRunning == LabRunningStatus.DISABLED && targetOpen == LabOpenStatus.OPEN) {
			if (openRequested) {
				throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
						"停用的实验室不能设置为正常开放");
			}
			targetOpen = LabOpenStatus.TEMPORARY_CLOSED;
		}
		return new ResolvedLabStatus(targetRunning, targetOpen);
	}

	private LabRunningStatus parseRunningStatus(Integer status) {
		if (status == null) {
			return null;
		}
		try {
			return LabRunningStatus.fromCode(status);
		}
		catch (IllegalArgumentException ex) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "使用状态只能是1到4");
		}
	}

	private LabOpenStatus parseOpenStatus(Integer status) {
		if (status == null) {
			return null;
		}
		try {
			return LabOpenStatus.fromCode(status);
		}
		catch (IllegalArgumentException ex) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "开放状态只能是1到3");
		}
	}

	private List<LabReservation> listActiveReservationsIfNeeded(Lab lab, ResolvedLabStatus target) {
		if (lab.getRunningStatus() != LabRunningStatus.IN_USE || target.runningStatus() == LabRunningStatus.IN_USE) {
			return List.of();
		}
		List<LabReservation> activeReservations = labReservationRepository.listActiveCheckedInByLabIdForUpdate(lab.getId());
		if (activeReservations.isEmpty()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"实验室处于使用中，但不存在已签到未签退预约，不能执行强制结束使用");
		}
		return activeReservations;
	}

	private List<LabReservation> listFutureReservationsIfNeeded(Lab lab, ResolvedLabStatus target) {
		if (target.openStatus() == LabOpenStatus.OPEN && target.runningStatus() != LabRunningStatus.DISABLED) {
			return List.of();
		}
		LocalDateTime now = LocalDateTime.now();
		return labReservationRepository.listFuturePendingOrApprovedByLabId(lab.getId(), now.toLocalDate())
				.stream()
				.filter(reservation -> reservation.getReservationDate().atTime(reservation.getEndTime()).isAfter(now))
				.toList();
	}

	private void forceCheckOutReservations(List<LabReservation> reservations, LocalDateTime now) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		reservations.forEach(reservation -> reservation.forceCheckOut(now));
		labReservationRepository.updateBatch(reservations);
		reservations.forEach(reservation -> labOccupancyCacheManager.evict(reservation.getLabId(),
				reservation.getReservationDate()));
	}

	private void cancelFutureReservations(Lab lab, List<LabReservation> reservations, String cancelReason,
			LocalDateTime now) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		reservations.forEach(reservation -> reservation.cancelBySystem(cancelReason, now));
		labReservationRepository.updateBatch(reservations);
		reservations.forEach(reservation -> labOccupancyCacheManager.evict(reservation.getLabId(),
				reservation.getReservationDate()));
	}

	private String buildStatusChangeCancelReason(ResolvedLabStatus target) {
		if (target.openStatus() == LabOpenStatus.PERMANENT_CLOSED) {
			return "实验室已永久关闭，关联预约自动取消";
		}
		if (target.runningStatus() == LabRunningStatus.DISABLED) {
			return "实验室已停用，关联预约自动取消";
		}
		if (target.openStatus() == LabOpenStatus.TEMPORARY_CLOSED) {
			return "实验室已临时关闭，关联预约自动取消";
		}
		return "实验室状态调整，关联预约自动取消";
	}

	private void writeStatusChangeLog(Lab lab, LabStatusUpdateCommand command, LabRunningStatus fromRunning,
			LabOpenStatus fromOpen, ResolvedLabStatus target, int forcedCheckoutCount, int cancelledReservationCount,
			LocalDateTime now) {
		labStatusChangeLogRepository.save(LabStatusChangeLog.builder()
				.labId(lab.getId())
				.operatorId(command.getOperatorId())
				.fromRunningStatus(fromRunning)
				.toRunningStatus(target.runningStatus())
				.fromOpenStatus(fromOpen)
				.toOpenStatus(target.openStatus())
				.changeReason(command.getChangeReason().trim())
				.triggerSource(MANUAL_STATUS_CHANGE_SOURCE)
				.forcedCheckoutCount(forcedCheckoutCount)
				.cancelledReservationCount(cancelledReservationCount)
				.createdAt(now)
				.build());
	}

	private LabStatusUpdateResponse buildStatusUpdateResponse(boolean applied, LabDetailResponse lab,
			List<LabReservation> conflictedReservations, List<LabReservation> cancelledReservations,
			List<LabReservation> forcedCheckedOutReservations, String message) {
		return LabStatusUpdateResponse.builder()
				.applied(applied)
				.lab(lab)
				.conflictedReservations(toConflictedResponses(conflictedReservations))
				.cancelledReservations(toConflictedResponses(cancelledReservations))
				.forcedCheckedOutReservations(toConflictedResponses(forcedCheckedOutReservations))
				.message(message)
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

	private LabUpdateCommand mergeUpdateCommand(Lab lab, LabUpdateCommand command) {
		LabLocation mergedLocation = command.getLocation() == null ? null : mergeLocation(lab.getLocation(), command.getLocation());
		return LabUpdateCommand.builder()
				.labName(command.getLabName())
				.labTypeId(command.getLabTypeId())
				.location(mergedLocation)
				.capacity(command.getCapacity())
				.areaM2(command.getAreaM2())
				.dutyUserId(command.getDutyUserId())
				.description(command.getDescription())
				.build();
	}

	private LabLocation mergeLocation(LabLocation current, LabLocation requested) {
		return LabLocation.builder()
				.campus(requested.getCampus() == null ? valueOrNull(current, LabLocation::getCampus) : requested.getCampus())
				.building(requested.getBuilding() == null ? valueOrNull(current, LabLocation::getBuilding) : requested.getBuilding())
				.floor(requested.getFloor() == null ? valueOrNull(current, LabLocation::getFloor) : requested.getFloor())
				.roomNo(requested.getRoomNo() == null ? valueOrNull(current, LabLocation::getRoomNo) : requested.getRoomNo())
				.build();
	}

	private <T> T valueOrNull(LabLocation location, Function<LabLocation, T> getter) {
		return location == null ? null : getter.apply(location);
	}

	private LabDetailResponse buildDetail(Lab lab) {
		LabType labType = requireLabType(lab.getLabTypeId());
		String dutyUserName = lab.getDutyUserId() == null ? null : resolveDutyUserName(lab.getDutyUserId())
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.DUTY_USER_NOT_FOUND,
						"负责人用户不存在，dutyUserId=" + lab.getDutyUserId()));
		return LabManagementConverter.toLabDetailResponse(lab, labType.getLabTypeName(), dutyUserName,
				labScheduleCacheManager.listByLabId(lab.getId(), () -> labScheduleRepository.listByLabId(lab.getId())));
	}

	private Optional<String> resolveDutyUserName(Long dutyUserId) {
		if (dutyUserId == null) {
			return Optional.empty();
		}
		return personDirectoryPort.getByUserId(dutyUserId).map(PersonDirectoryItemDTO::getPersonName);
	}

	private Map<Long, String> mapDutyUserNames(Collection<Long> dutyUserIds) {
		if (dutyUserIds == null || dutyUserIds.isEmpty()) {
			return Map.of();
		}
		return dutyUserIds.stream()
				.filter(id -> id != null)
				.distinct()
				.collect(Collectors.toMap(Function.identity(), userId -> resolveDutyUserName(userId)
						.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.DUTY_USER_NOT_FOUND,
								"负责人用户不存在，dutyUserId=" + userId))));
	}

	private void publishReservationCancelledMessages(Lab lab, List<LabReservation> reservations) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		for (LabReservation reservation : reservations) {
			// TODO 后续预约模块稳定后改为使用正式消息模板编码，而不是临时模板。
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
					.build());
		}
	}

	private void publishForcedCheckoutMessages(Lab lab, List<LabReservation> reservations, String reason) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		for (LabReservation reservation : reservations) {
			messagePublisher.publish(MessagePublishCommand.builder()
					.eventNo(MessageEventNoGenerator.generate(LabManagementConstants.RESERVATION_FORCED_CHECKOUT_MESSAGE_TYPE))
					.sourceSystem(LabManagementConstants.SOURCE_SYSTEM)
					.messageType(LabManagementConstants.RESERVATION_FORCED_CHECKOUT_MESSAGE_TYPE)
					.targetType(TargetType.SINGLE)
					.targetUserIds(List.of(reservation.getApplicantId()))
					.tempTemplate(TempMessageTemplate.builder()
							.titleTemplate("实验室使用已被强制结束")
							.contentTemplate(buildForcedCheckoutContent(lab, reservation, reason))
							.contentFormat(ContentFormat.TEXT)
							.build())
					.occurredAt(LocalDateTime.now())
					.build());
		}
	}

	private void publishLabStatusChangedMessage(Lab lab, LabRunningStatus fromRunning, LabOpenStatus fromOpen,
			ResolvedLabStatus target, String reason) {
		if (lab.getDutyUserId() == null) {
			return;
		}
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo(MessageEventNoGenerator.generate(LabManagementConstants.LAB_STATUS_CHANGED_MESSAGE_TYPE))
				.sourceSystem(LabManagementConstants.SOURCE_SYSTEM)
				.messageType(LabManagementConstants.LAB_STATUS_CHANGED_MESSAGE_TYPE)
				.targetType(TargetType.SINGLE)
				.targetUserIds(List.of(lab.getDutyUserId()))
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("实验室状态已变更")
						.contentTemplate(buildLabStatusChangedContent(lab, fromRunning, fromOpen, target, reason))
						.contentFormat(ContentFormat.TEXT)
						.build())
				.occurredAt(LocalDateTime.now())
				.build());
	}

	private String buildForcedCheckoutContent(Lab lab, LabReservation reservation, String reason) {
		return "你的实验室使用已被管理员强制结束。"
				+ "\n实验室：" + lab.getLabName()
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime()
				+ "\n原因：" + reason;
	}

	private String buildLabStatusChangedContent(Lab lab, LabRunningStatus fromRunning, LabOpenStatus fromOpen,
			ResolvedLabStatus target, String reason) {
		return "你负责的实验室状态已变更。"
				+ "\n实验室：" + lab.getLabName()
				+ "\n原状态：" + fromRunning.getDescription() + "/" + fromOpen.getDescription()
				+ "\n新状态：" + target.runningStatus().getDescription() + "/" + target.openStatus().getDescription()
				+ "\n原因：" + reason;
	}

	private String buildReservationCancelledContent(Lab lab, LabReservation reservation) {
		return buildReservationCancelledPrefix(reservation)
				+ "\n实验室：" + lab.getLabName()
				+ "\n预约单号：" + reservation.getReservationNo()
				+ "\n预约日期：" + reservation.getReservationDate()
				+ "\n预约时间：" + reservation.getStartTime() + "-" + reservation.getEndTime()
				+ "\n取消原因：" + reservation.getCancelReason();
	}

	private String buildReservationCancelledPrefix(LabReservation reservation) {
		if (reservation.getCancelReason() != null && reservation.getCancelReason().contains("删除")) {
			return "你预约的实验室已被管理员删除，预约已自动取消。";
		}
		return "你预约的实验室因状态调整，预约已自动取消。";
	}

	private void publishLabInfoRealtime(String eventName, Lab lab) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("labId", lab.getId());
		payload.put("labName", lab.getLabName());
		payload.put("labTypeId", lab.getLabTypeId());
		payload.put("dutyUserId", lab.getDutyUserId());
		payload.put("runningStatus", lab.getRunningStatus() == null ? null : lab.getRunningStatus().getCode());
		payload.put("openStatus", lab.getOpenStatus() == null ? null : lab.getOpenStatus().getCode());
		realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
				.event(eventName)
				.source(LabManagementConstants.SOURCE_SYSTEM)
				.targetType(RealtimeTargetType.BROADCAST)
				.data(payload)
				.occurredAt(LocalDateTime.now())
				.build());
	}

	private void publishLabStatusRealtime(Lab lab, LabRunningStatus fromRunning, LabOpenStatus fromOpen,
			ResolvedLabStatus target, String reason, int forcedCheckoutCount, int cancelledReservationCount) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("labId", lab.getId());
		payload.put("labName", lab.getLabName());
		payload.put("fromRunningStatus", fromRunning.getCode());
		payload.put("fromRunningStatusDesc", fromRunning.getDescription());
		payload.put("fromOpenStatus", fromOpen.getCode());
		payload.put("fromOpenStatusDesc", fromOpen.getDescription());
		payload.put("runningStatus", target.runningStatus().getCode());
		payload.put("runningStatusDesc", target.runningStatus().getDescription());
		payload.put("openStatus", target.openStatus().getCode());
		payload.put("openStatusDesc", target.openStatus().getDescription());
		payload.put("changeReason", reason);
		payload.put("forcedCheckoutCount", forcedCheckoutCount);
		payload.put("cancelledReservationCount", cancelledReservationCount);
		realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
				.event("lab.status_changed")
				.source(LabManagementConstants.SOURCE_SYSTEM)
				.targetType(RealtimeTargetType.BROADCAST)
				.data(payload)
				.occurredAt(LocalDateTime.now())
				.build());
	}

	private record ResolvedLabStatus(LabRunningStatus runningStatus, LabOpenStatus openStatus) {
	}
}
