package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

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
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationApproveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCancelCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

	@Mock
	private LabRepository labRepository;
	@Mock
	private LabReservationRepository labReservationRepository;
	@Mock
	private LabScheduleRepository labScheduleRepository;
	@Mock
	private LabCloseScheduleRepository labCloseScheduleRepository;
	@Mock
	private LabStatusChangeLogRepository labStatusChangeLogRepository;
	@Mock
	private PersonDirectoryPort personDirectoryPort;
	@Mock
	private MessagePublisher messagePublisher;
	@Mock
	private RealtimePublisher realtimePublisher;
	@Mock
	private LabOccupancyCacheManager labOccupancyCacheManager;
	@Mock
	private LabScheduleCacheManager labScheduleCacheManager;

	@InjectMocks
	private ReservationServiceImpl reservationService;

	@Test
	void createShouldSavePendingReservationAndEvictOccupancy() {
		LocalDate date = LocalDate.now().plusDays(1);
		ReservationCreateCommand command = createCommand(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN)));
		when(labScheduleCacheManager.listByLabId(eq(100L), any()))
				.thenReturn(List.of(schedule(date, LocalTime.of(8, 0), LocalTime.of(18, 0))));
		when(labCloseScheduleRepository.existsOverlap(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(false);
		when(labReservationRepository.existsActiveOverlap(100L, date, LocalTime.of(9, 0), LocalTime.of(10, 0)))
				.thenReturn(false);
		when(labReservationRepository.existsByReservationNo(anyString())).thenReturn(false);
		when(labReservationRepository.save(any(LabReservation.class))).thenAnswer(invocation -> {
			LabReservation reservation = invocation.getArgument(0);
			reservation.setId(200L);
			return reservation;
		});
		stubResponseLookups(10L);

		ReservationResponse response = reservationService.create(command);

		ArgumentCaptor<LabReservation> reservationCaptor = ArgumentCaptor.forClass(LabReservation.class);
		verify(labReservationRepository).save(reservationCaptor.capture());
		assertThat(reservationCaptor.getValue().getReservationStatus()).isEqualTo(LabReservationStatus.PENDING_APPROVAL);
		assertThat(reservationCaptor.getValue().getReservationNo()).startsWith("LAB_RESV-" + date.toString().replace("-", ""));
		verify(labOccupancyCacheManager).evict(100L, date);
		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.PENDING_APPROVAL.getCode());
		assertThat(response.getLabName()).isEqualTo("机器人实验室");
	}

	@Test
	void createShouldUseDefaultOpenScheduleWhenLabHasNoScheduleConfigured() {
		LocalDate date = LocalDate.now().plusDays(1);
		ReservationCreateCommand command = createCommand(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN)));
		when(labScheduleCacheManager.listByLabId(eq(100L), any())).thenReturn(List.of());
		when(labCloseScheduleRepository.existsOverlap(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(false);
		when(labReservationRepository.existsActiveOverlap(100L, date, LocalTime.of(9, 0), LocalTime.of(10, 0)))
				.thenReturn(false);
		when(labReservationRepository.existsByReservationNo(anyString())).thenReturn(false);
		when(labReservationRepository.save(any(LabReservation.class))).thenAnswer(invocation -> {
			LabReservation reservation = invocation.getArgument(0);
			reservation.setId(200L);
			return reservation;
		});
		stubResponseLookups(10L);

		ReservationResponse response = reservationService.create(command);

		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.PENDING_APPROVAL.getCode());
		verify(labReservationRepository).save(any(LabReservation.class));
	}

	@Test
	void createShouldRejectActiveReservationConflict() {
		LocalDate date = LocalDate.now().plusDays(1);
		ReservationCreateCommand command = createCommand(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN)));
		when(labScheduleCacheManager.listByLabId(eq(100L), any()))
				.thenReturn(List.of(schedule(date, LocalTime.of(8, 0), LocalTime.of(18, 0))));
		when(labCloseScheduleRepository.existsOverlap(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(false);
		when(labReservationRepository.existsActiveOverlap(100L, date, LocalTime.of(9, 0), LocalTime.of(10, 0)))
				.thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(command))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.RESERVATION_TIME_CONFLICT.getCode());
		verify(labReservationRepository, never()).save(any());
	}

	@Test
	void approveShouldUpdateReservationAndPublishMessage() {
		LabReservation reservation = pendingReservation();
		when(labReservationRepository.findByReservationNoForUpdate("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labReservationRepository.update(any(LabReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		stubResponseLookups(10L, 99L);

		ReservationResponse response = reservationService.approve(ReservationApproveCommand.builder()
				.reservationNo("LAB_RESV-20260709123456")
				.approverId(99L)
				.approved(true)
				.approveComment("通过")
				.build());

		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.APPROVED.getCode());
		assertThat(response.getApproverName()).isEqualTo("管理员");
		ArgumentCaptor<MessagePublishCommand> messageCaptor = ArgumentCaptor.forClass(MessagePublishCommand.class);
		verify(messagePublisher).publish(messageCaptor.capture());
		assertThat(messageCaptor.getValue().getMessageType()).isEqualTo("lab_management.system.reservation_approved");
		assertThat(messageCaptor.getValue().getTargetUserIds()).containsExactly(10L);
		verify(labOccupancyCacheManager).evict(100L, reservation.getReservationDate());
	}

	@Test
	void cancelShouldRejectNonApplicant() {
		when(labReservationRepository.findByReservationNoForUpdate("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(pendingReservation()));

		assertThatThrownBy(() -> reservationService.cancel(ReservationCancelCommand.builder()
				.reservationNo("LAB_RESV-20260709123456")
				.applicantId(11L)
				.cancelReason("临时有事")
				.build()))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.RESERVATION_PERMISSION_DENIED.getCode());
		verify(labReservationRepository, never()).update(any());
	}

	@Test
	void checkInShouldSetReservationCheckedInAndLabInUse() {
		LocalDateTime now = LocalDateTime.now();
		LocalTime startTime = now.toLocalTime().minusMinutes(1);
		LocalTime endTime = now.toLocalTime().plusMinutes(30);
		if (!startTime.isBefore(now.toLocalTime())) {
			startTime = LocalTime.MIN;
		}
		if (!endTime.isAfter(startTime)) {
			endTime = LocalTime.MAX;
		}
		LabReservation reservation = approvedReservation(now.toLocalDate(), startTime, endTime);
		Lab lab = lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN);
		when(labReservationRepository.findByReservationNo("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.findByReservationNoForUpdate("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labReservationRepository.update(any(LabReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		stubResponseLookups(10L);

		ReservationResponse response = reservationService.checkIn("LAB_RESV-20260709123456", 10L);

		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.CHECKED_IN.getCode());
		assertThat(response.getCheckInAt()).isNotNull();
		assertThat(lab.getRunningStatus()).isEqualTo(LabRunningStatus.IN_USE);
		verify(labRepository).updateStatus(lab);
		verify(labOccupancyCacheManager).evict(100L, reservation.getReservationDate());
	}

	@Test
	void adminCheckInShouldNotRequireApplicantAndShouldWriteStatusLog() {
		LocalDateTime now = LocalDateTime.now();
		LocalTime startTime = now.toLocalTime().minusMinutes(1);
		LocalTime endTime = now.toLocalTime().plusMinutes(30);
		if (!startTime.isBefore(now.toLocalTime())) {
			startTime = LocalTime.MIN;
		}
		if (!endTime.isAfter(startTime)) {
			endTime = LocalTime.MAX;
		}
		LabReservation reservation = approvedReservation(now.toLocalDate(), startTime, endTime);
		Lab lab = lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN);
		when(labReservationRepository.findByReservationNo("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.findByReservationNoForUpdate("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labReservationRepository.update(any(LabReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		stubResponseLookups(10L);

		ReservationResponse response = reservationService.adminCheckIn("LAB_RESV-20260709123456", 99L);

		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.CHECKED_IN.getCode());
		assertThat(lab.getRunningStatus()).isEqualTo(LabRunningStatus.IN_USE);
		verify(labRepository).updateStatus(lab);
		ArgumentCaptor<LabStatusChangeLog> logCaptor = ArgumentCaptor.forClass(LabStatusChangeLog.class);
		verify(labStatusChangeLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getOperatorId()).isEqualTo(99L);
		assertThat(logCaptor.getValue().getFromRunningStatus()).isEqualTo(LabRunningStatus.IDLE);
		assertThat(logCaptor.getValue().getToRunningStatus()).isEqualTo(LabRunningStatus.IN_USE);
		assertThat(logCaptor.getValue().getChangeReason()).contains("管理员帮签");
	}

	@Test
	void checkOutShouldKeepCheckedInStatusAndSetLabIdle() {
		LocalDateTime now = LocalDateTime.now();
		LocalTime endTime = now.toLocalTime().minusMinutes(1);
		if (!endTime.isBefore(now.toLocalTime())) {
			endTime = LocalTime.MIN;
		}
		LabReservation reservation = checkedInReservation(now.toLocalDate(), LocalTime.MIN, endTime);
		Lab lab = lab(LabRunningStatus.IN_USE, LabOpenStatus.TEMPORARY_CLOSED);
		when(labReservationRepository.findByReservationNo("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.findByReservationNoForUpdate("LAB_RESV-20260709123456"))
				.thenReturn(Optional.of(reservation));
		when(labReservationRepository.update(any(LabReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		stubResponseLookups(10L);

		ReservationResponse response = reservationService.checkOut("LAB_RESV-20260709123456", 10L);

		assertThat(response.getReservationStatus()).isEqualTo(LabReservationStatus.CHECKED_IN.getCode());
		assertThat(response.getCheckOutAt()).isNotNull();
		assertThat(lab.getRunningStatus()).isEqualTo(LabRunningStatus.IDLE);
		assertThat(lab.getOpenStatus()).isEqualTo(LabOpenStatus.TEMPORARY_CLOSED);
		verify(labRepository).updateStatus(lab);
	}

	@Test
	void markAbsentReservationsShouldUpdateEvictAndPublishMessage() {
		LocalDateTime now = LocalDateTime.now();
		LocalTime startTime = now.toLocalTime().minusMinutes(40);
		if (!startTime.isBefore(now.toLocalTime())) {
			startTime = LocalTime.MIN;
		}
		LocalTime endTime = now.toLocalTime().plusMinutes(20);
		if (!endTime.isAfter(startTime)) {
			endTime = LocalTime.MAX;
		}
		LabReservation reservation = approvedReservation(now.toLocalDate(), startTime, endTime);
		when(labReservationRepository.listApprovedUncheckedByDate(now.toLocalDate())).thenReturn(List.of(reservation));
		stubLabLookup();

		reservationService.markAbsentReservations();

		ArgumentCaptor<List<LabReservation>> reservationsCaptor = ArgumentCaptor.forClass(List.class);
		verify(labReservationRepository).updateBatch(reservationsCaptor.capture());
		assertThat(reservationsCaptor.getValue().getFirst().getReservationStatus()).isEqualTo(LabReservationStatus.ABSENT);
		verify(labOccupancyCacheManager).evict(100L, now.toLocalDate());
		ArgumentCaptor<MessagePublishCommand> messageCaptor = ArgumentCaptor.forClass(MessagePublishCommand.class);
		verify(messagePublisher).publish(messageCaptor.capture());
		assertThat(messageCaptor.getValue().getMessageType()).isEqualTo("lab_management.system.reservation_absent");
	}

	private ReservationCreateCommand createCommand(LocalDate date, LocalTime startTime, LocalTime endTime) {
		return ReservationCreateCommand.builder()
				.labId(100L)
				.applicantId(10L)
				.reservationDate(date)
				.startTime(startTime)
				.endTime(endTime)
				.purpose("机器学习实验")
				.courseName("人工智能导论")
				.build();
	}

	private Lab lab(LabRunningStatus runningStatus, LabOpenStatus openStatus) {
		return Lab.builder()
				.id(100L)
				.labName("机器人实验室")
				.runningStatus(runningStatus)
				.openStatus(openStatus)
				.build();
	}

	private LabWeeklySchedule schedule(LocalDate date, LocalTime openTime, LocalTime closeTime) {
		return LabWeeklySchedule.builder()
				.labId(100L)
				.dayOfWeek(date.getDayOfWeek().getValue())
				.openTime(openTime)
				.closeTime(closeTime)
				.build();
	}

	private LabReservation pendingReservation() {
		return LabReservation.builder()
				.id(200L)
				.reservationNo("LAB_RESV-20260709123456")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(LocalDate.now().plusDays(1))
				.startTime(LocalTime.of(9, 0))
				.endTime(LocalTime.of(10, 0))
				.reservationStatus(LabReservationStatus.PENDING_APPROVAL)
				.build();
	}

	private LabReservation approvedReservation(LocalDate date, LocalTime startTime, LocalTime endTime) {
		return LabReservation.builder()
				.id(200L)
				.reservationNo("LAB_RESV-20260709123456")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(date)
				.startTime(startTime)
				.endTime(endTime)
				.reservationStatus(LabReservationStatus.APPROVED)
				.build();
	}

	private LabReservation checkedInReservation(LocalDate date, LocalTime startTime, LocalTime endTime) {
		LabReservation reservation = approvedReservation(date, startTime, endTime);
		reservation.setReservationStatus(LabReservationStatus.CHECKED_IN);
		reservation.setCheckInAt(LocalDateTime.now().minusHours(1));
		return reservation;
	}

	private void stubResponseLookups(Long... userIds) {
		stubLabLookup();
		for (Long userId : userIds) {
			when(personDirectoryPort.getByUserId(userId)).thenReturn(Optional.of(person(userId)));
		}
	}

	private void stubLabLookup() {
		when(labRepository.mapByIdsIncludingDeleted(List.of(100L)))
				.thenReturn(Map.of(100L, lab(LabRunningStatus.IDLE, LabOpenStatus.OPEN)));
	}

	private PersonDirectoryItemDTO person(Long userId) {
		return PersonDirectoryItemDTO.builder()
				.userId(userId)
				.personName(userId == 99L ? "管理员" : "陆喜言")
				.build();
	}
}
