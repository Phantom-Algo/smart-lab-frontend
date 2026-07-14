package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabCloseScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabCloseType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
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
class LabCloseScheduleServiceImplTest {

	@Mock
	private LabRepository labRepository;
	@Mock
	private LabCloseScheduleRepository labCloseScheduleRepository;
	@Mock
	private LabReservationRepository labReservationRepository;
	@Mock
	private LabOccupancyCacheManager labOccupancyCacheManager;
	@Mock
	private MessagePublisher messagePublisher;

	@InjectMocks
	private LabCloseScheduleServiceImpl labCloseScheduleService;

	@Test
	void createShouldReturnConflictsWithoutApplyingWhenForceFalse() {
		LocalDate date = LocalDate.now().plusDays(1);
		LabReservation reservation = reservation(date);
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		when(labReservationRepository.listPendingOrApprovedByLabIdAndDateRange(100L, date, date))
				.thenReturn(List.of(reservation));

		LabCloseScheduleMutationResponse response = labCloseScheduleService.create(100L,
				command(false, date.atTime(9, 0), date.atTime(11, 0)));

		assertThat(response.isApplied()).isFalse();
		assertThat(response.getConflictedReservations()).extracting("reservationNo").containsExactly("LAB_RESV-1");
		verify(labCloseScheduleRepository, never()).save(any());
		verify(labReservationRepository, never()).updateBatch(anyList());
	}

	@Test
	void createShouldSaveAndCancelConflictsWhenForced() {
		LocalDate date = LocalDate.now().plusDays(1);
		LabReservation reservation = reservation(date);
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		when(labReservationRepository.listPendingOrApprovedByLabIdAndDateRange(100L, date, date))
				.thenReturn(List.of(reservation));
		when(labCloseScheduleRepository.save(any(LabCloseSchedule.class))).thenAnswer(invocation -> {
			LabCloseSchedule schedule = invocation.getArgument(0);
			schedule.setId(300L);
			return schedule;
		});

		LabCloseScheduleMutationResponse response = labCloseScheduleService.create(100L,
				command(true, date.atTime(9, 0), date.atTime(11, 0)));

		assertThat(response.isApplied()).isTrue();
		assertThat(response.getCloseSchedule().getId()).isEqualTo(300L);
		ArgumentCaptor<List<LabReservation>> reservationCaptor = ArgumentCaptor.forClass(List.class);
		verify(labReservationRepository).updateBatch(reservationCaptor.capture());
		assertThat(reservationCaptor.getValue().getFirst().getReservationStatus()).isEqualTo(LabReservationStatus.CANCELLED);
		assertThat(reservationCaptor.getValue().getFirst().getCancelReason()).contains("设备维保");
		verify(labOccupancyCacheManager).evict(100L, date);
		verify(messagePublisher).publish(any(MessagePublishCommand.class));
	}

	@Test
	void createShouldRejectInvalidTimeRange() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab()));
		LocalDateTime now = LocalDateTime.now().plusDays(1);

		assertThatThrownBy(() -> labCloseScheduleService.create(100L,
				command(false, now, now.minusMinutes(1))))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID.getCode());
		verify(labCloseScheduleRepository, never()).save(any());
	}

	@Test
	void listShouldRejectInvalidDateRange() {
		when(labRepository.findById(100L)).thenReturn(Optional.of(lab()));

		assertThatThrownBy(() -> labCloseScheduleService.listByLabId(100L,
				LabCloseScheduleQueryCriteria.builder()
						.startDate(LocalDate.of(2026, 7, 10))
						.endDate(LocalDate.of(2026, 7, 9))
						.build()))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_CLOSE_SCHEDULE_PARAM_INVALID.getCode());
		verify(labCloseScheduleRepository, never()).listByLabId(eq(100L), any());
	}

	private Lab lab() {
		return Lab.builder()
				.id(100L)
				.labName("机器人实验室")
				.runningStatus(LabRunningStatus.IDLE)
				.openStatus(LabOpenStatus.OPEN)
				.build();
	}

	private LabCloseScheduleCreateCommand command(boolean force, LocalDateTime startTime, LocalDateTime endTime) {
		return LabCloseScheduleCreateCommand.builder()
				.closeType(LabCloseType.EQUIPMENT_MAINTENANCE.getCode())
				.startTime(startTime)
				.endTime(endTime)
				.reason("检修")
				.forceCancelConflictedReservations(force)
				.build();
	}

	private LabReservation reservation(LocalDate date) {
		return LabReservation.builder()
				.id(200L)
				.reservationNo("LAB_RESV-1")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(date)
				.startTime(LocalTime.of(10, 0))
				.endTime(LocalTime.of(12, 0))
				.reservationStatus(LabReservationStatus.APPROVED)
				.build();
	}
}
