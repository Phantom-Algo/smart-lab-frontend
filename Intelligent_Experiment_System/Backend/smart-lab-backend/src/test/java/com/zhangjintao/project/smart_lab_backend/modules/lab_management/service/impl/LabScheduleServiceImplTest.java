package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleItemCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleSaveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabScheduleServiceImplTest {

	@Mock
	private LabRepository labRepository;
	@Mock
	private LabScheduleRepository labScheduleRepository;
	@Mock
	private LabReservationRepository labReservationRepository;
	@Mock
	private LabScheduleCacheManager labScheduleCacheManager;
	@Mock
	private LabOccupancyCacheManager labOccupancyCacheManager;
	@Mock
	private MessagePublisher messagePublisher;

	@InjectMocks
	private LabScheduleServiceImpl labScheduleService;

	@Test
	void saveShouldReturnConflictsWithoutApplyingWhenForceFalse() {
		LocalDate date = nextDateWithDay(1);
		LabReservation reservation = reservation(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		when(labReservationRepository.listFuturePendingOrApprovedByLabId(eq(100L), any(LocalDate.class)))
				.thenReturn(List.of(reservation));
		when(labScheduleRepository.listByLabId(100L)).thenReturn(List.of(schedule(1, 8, 18)));

		LabScheduleMutationResponse response = labScheduleService.save(100L,
				command(false, item(1, LocalTime.of(10, 0), LocalTime.of(12, 0))));

		assertThat(response.isApplied()).isFalse();
		assertThat(response.getConflictedReservations()).extracting("reservationNo").containsExactly("LAB_RESV-1");
		verify(labScheduleRepository, never()).upsertBatch(anyList());
		verify(labReservationRepository, never()).updateBatch(anyList());
	}

	@Test
	void saveShouldUpsertAndCancelConflictsWhenForced() {
		LocalDate date = nextDateWithDay(1);
		LabReservation reservation = reservation(date, LocalTime.of(9, 0), LocalTime.of(10, 0));
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		when(labReservationRepository.listFuturePendingOrApprovedByLabId(eq(100L), any(LocalDate.class)))
				.thenReturn(List.of(reservation));
		when(labScheduleRepository.listByLabId(100L)).thenReturn(List.of(schedule(1, 10, 12)));

		LabScheduleMutationResponse response = labScheduleService.save(100L,
				command(true, item(1, LocalTime.of(10, 0), LocalTime.of(12, 0))));

		assertThat(response.isApplied()).isTrue();
		verify(labScheduleRepository).upsertBatch(anyList());
		ArgumentCaptor<List<LabReservation>> reservationCaptor = ArgumentCaptor.forClass(List.class);
		verify(labReservationRepository).updateBatch(reservationCaptor.capture());
		assertThat(reservationCaptor.getValue().getFirst().getReservationStatus()).isEqualTo(LabReservationStatus.CANCELLED);
		assertThat(reservationCaptor.getValue().getFirst().getCancelReason()).contains("开放时间调整");
		verify(labOccupancyCacheManager).evict(100L, date);
		verify(messagePublisher).publish(any(MessagePublishCommand.class));
		verify(labScheduleCacheManager).evict(100L);
	}

	@Test
	void saveShouldRejectDuplicateDay() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));

		assertThatThrownBy(() -> labScheduleService.save(100L,
				command(false, item(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
						item(1, LocalTime.of(13, 0), LocalTime.of(18, 0)))))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_SCHEDULE_PARAM_INVALID.getCode());
		verify(labScheduleRepository, never()).upsertBatch(anyList());
	}

	@Test
	void deleteShouldRejectMissingSchedule() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		when(labScheduleRepository.findByLabIdAndId(100L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labScheduleService.delete(100L, 1L, false))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_SCHEDULE_NOT_FOUND.getCode());
		verify(labScheduleRepository, never()).deleteById(any());
	}

	private Lab lab() {
		return Lab.builder()
				.id(100L)
				.labName("机器人实验室")
				.runningStatus(LabRunningStatus.DISABLED)
				.openStatus(LabOpenStatus.TEMPORARY_CLOSED)
				.build();
	}

	private LabScheduleSaveCommand command(boolean force, LabScheduleItemCommand... items) {
		return LabScheduleSaveCommand.builder()
				.schedules(List.of(items))
				.forceCancelConflictedReservations(force)
				.build();
	}

	private LabScheduleItemCommand item(Integer dayOfWeek, LocalTime openTime, LocalTime closeTime) {
		return LabScheduleItemCommand.builder()
				.dayOfWeek(dayOfWeek)
				.openTime(openTime)
				.closeTime(closeTime)
				.build();
	}

	private LabWeeklySchedule schedule(Integer dayOfWeek, int openHour, int closeHour) {
		return LabWeeklySchedule.builder()
				.id(1L)
				.labId(100L)
				.dayOfWeek(dayOfWeek)
				.openTime(LocalTime.of(openHour, 0))
				.closeTime(LocalTime.of(closeHour, 0))
				.build();
	}

	private LabReservation reservation(LocalDate date, LocalTime startTime, LocalTime endTime) {
		return LabReservation.builder()
				.id(200L)
				.reservationNo("LAB_RESV-1")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(date)
				.startTime(startTime)
				.endTime(endTime)
				.reservationStatus(LabReservationStatus.APPROVED)
				.build();
	}

	private LocalDate nextDateWithDay(int dayOfWeek) {
		LocalDate today = LocalDate.now();
		int diff = dayOfWeek - today.getDayOfWeek().getValue();
		if (diff <= 0) {
			diff += 7;
		}
		return today.plusDays(diff);
	}
}
