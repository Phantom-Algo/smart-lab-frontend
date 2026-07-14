package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
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
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabStatusUpdateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabOccupancyCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorBindingService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabServiceImplTest {

	@Mock
	private LabRepository labRepository;
	@Mock
	private LabTypeRepository labTypeRepository;
	@Mock
	private LabScheduleRepository labScheduleRepository;
	@Mock
	private LabReservationRepository labReservationRepository;
	@Mock
	private LabStatusChangeLogRepository labStatusChangeLogRepository;
	@Mock
	private LabScheduleCacheManager labScheduleCacheManager;
	@Mock
	private LabOccupancyCacheManager labOccupancyCacheManager;
	@Mock
	private LabSensorBindingService labSensorBindingService;
	@Mock
	private PersonDirectoryPort personDirectoryPort;
	@Mock
	private MessagePublisher messagePublisher;

	@InjectMocks
	private LabServiceImpl labService;

	@Test
	void createShouldUseDefaultDisabledAndTemporaryClosedStatus() {
		LabCreateCommand command = createCommand();
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType()));
		when(labRepository.existsByLocation(command.getLocation(), null)).thenReturn(false);
		when(labRepository.save(any(Lab.class))).thenAnswer(invocation -> {
			Lab lab = invocation.getArgument(0);
			lab.setId(100L);
			lab.setCreatedAt(LocalDateTime.of(2026, 7, 8, 10, 0));
			lab.setUpdatedAt(LocalDateTime.of(2026, 7, 8, 10, 0));
			return lab;
		});
		when(labScheduleCacheManager.listByLabId(eq(100L), any())).thenReturn(List.of());

		LabDetailResponse response = labService.create(command);

		ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
		verify(labRepository).save(labCaptor.capture());
		assertThat(labCaptor.getValue().getRunningStatus()).isEqualTo(LabRunningStatus.DISABLED);
		assertThat(labCaptor.getValue().getOpenStatus()).isEqualTo(LabOpenStatus.TEMPORARY_CLOSED);
		assertThat(response.getRunningStatus()).isEqualTo(4);
		assertThat(response.getOpenStatus()).isEqualTo(2);
		assertThat(response.getCapacity()).isEqualTo(0);
	}

	@Test
	void createShouldRejectMissingLabType() {
		LabCreateCommand command = createCommand();
		when(labTypeRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labService.create(command))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_TYPE_NOT_FOUND.getCode());
		verify(labRepository, never()).save(any());
	}

	@Test
	void createShouldRejectMissingDutyUser() {
		LabCreateCommand command = LabCreateCommand.builder()
				.labName("AI实验室")
				.labTypeId(1L)
				.location(location())
				.capacity(30)
				.dutyUserId(10L)
				.build();
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType()));
		when(personDirectoryPort.getByUserId(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labService.create(command))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.DUTY_USER_NOT_FOUND.getCode());
		verify(labRepository, never()).save(any());
	}

	@Test
	void createShouldRejectDuplicateCompleteLocation() {
		LabCreateCommand command = createCommand();
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType()));
		when(labRepository.existsByLocation(command.getLocation(), null)).thenReturn(true);

		assertThatThrownBy(() -> labService.create(command))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_LOCATION_DUPLICATE.getCode());
		verify(labRepository, never()).save(any());
	}

	@Test
	void createShouldRejectInvalidCapacity() {
		LabCreateCommand command = LabCreateCommand.builder()
				.labName("AI实验室")
				.labTypeId(1L)
				.location(location())
				.capacity(-1)
				.build();

		assertThatThrownBy(() -> labService.create(command))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_PARAM_INVALID.getCode());
		verify(labTypeRepository, never()).findById(any());
	}

	@Test
	void updateShouldKeepNullFieldsAndMergePartialLocation() {
		Lab existing = existingLab();
		LabUpdateCommand command = LabUpdateCommand.builder()
				.location(LabLocation.builder().floor(3).build())
				.build();
		when(labRepository.findById(100L)).thenReturn(Optional.of(existing));
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType()));
		when(labRepository.existsByLocation(any(LabLocation.class), eq(100L))).thenReturn(false);
		when(labRepository.update(any(Lab.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(labScheduleCacheManager.listByLabId(eq(100L), any())).thenReturn(List.of());

		LabDetailResponse response = labService.update(100L, command);

		ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
		verify(labRepository).update(labCaptor.capture());
		assertThat(labCaptor.getValue().getLabName()).isEqualTo("机器人实验室");
		assertThat(labCaptor.getValue().getCapacity()).isEqualTo(40);
		assertThat(labCaptor.getValue().getLocation().getCampus()).isEqualTo("南校区");
		assertThat(labCaptor.getValue().getLocation().getFloor()).isEqualTo(3);
		assertThat(response.getFloor()).isEqualTo(3);
	}

	@Test
	void pageShouldFillLabTypeNameAndDutyUserName() {
		LabPageItemDTO item = LabPageItemDTO.builder()
				.id(100L)
				.labName("机器人实验室")
				.labTypeId(1L)
				.location(location())
				.capacity(40)
				.areaM2(BigDecimal.valueOf(90))
				.dutyUserId(10L)
				.runningStatus(LabRunningStatus.IDLE)
				.openStatus(LabOpenStatus.OPEN)
				.description("用于机器人实验")
				.createdAt(LocalDateTime.of(2026, 7, 8, 10, 0))
				.build();
		when(labRepository.page(any(LabQueryCriteria.class))).thenReturn(PageResult.of(List.of(item), 1, 1, 20));
		when(labTypeRepository.mapByIds(List.of(1L))).thenReturn(Map.of(1L, labType()));
		when(personDirectoryPort.getByUserId(10L)).thenReturn(Optional.of(person()));

		PageResult<LabResponse> page = labService.page(LabQueryCriteria.builder().page(1L).pageSize(20L).build());

		assertThat(page.getRecords()).hasSize(1);
		assertThat(page.getRecords().getFirst().getLabTypeName()).isEqualTo("人工智能实验室");
		assertThat(page.getRecords().getFirst().getDutyUserName()).isEqualTo("陆喜言");
	}

	@Test
	void deleteShouldCancelFutureReservationsAndPublishMessage() {
		Lab lab = existingLab();
		LabReservation reservation = LabReservation.builder()
				.id(200L)
				.reservationNo("LR202607080001")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(LocalDate.now().plusDays(1))
				.startTime(LocalTime.of(9, 0))
				.endTime(LocalTime.of(10, 0))
				.reservationStatus(LabReservationStatus.APPROVED)
				.build();
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.listFuturePendingOrApprovedByLabId(eq(100L), any(LocalDate.class)))
				.thenReturn(List.of(reservation));

		labService.delete(100L);

		ArgumentCaptor<List<LabReservation>> reservationsCaptor = ArgumentCaptor.forClass(List.class);
		verify(labReservationRepository).updateBatch(reservationsCaptor.capture());
		assertThat(reservationsCaptor.getValue().getFirst().getReservationStatus()).isEqualTo(LabReservationStatus.CANCELLED);
		assertThat(reservationsCaptor.getValue().getFirst().getCancelReason()).contains("实验室已被删除");

		ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
		verify(labRepository).softDelete(labCaptor.capture());
		assertThat(labCaptor.getValue().getRunningStatus()).isEqualTo(LabRunningStatus.DISABLED);
		assertThat(labCaptor.getValue().getOpenStatus()).isEqualTo(LabOpenStatus.PERMANENT_CLOSED);
		assertThat(labCaptor.getValue().getDeletedAt()).isNotNull();

		ArgumentCaptor<MessagePublishCommand> messageCaptor = ArgumentCaptor.forClass(MessagePublishCommand.class);
		verify(messagePublisher).publish(messageCaptor.capture());
		assertThat(messageCaptor.getValue().getMessageType()).isEqualTo("lab_management.system.reservation_cancelled");
		assertThat(messageCaptor.getValue().getTargetUserIds()).containsExactly(10L);
		assertThat(messageCaptor.getValue().getTempTemplate().getTitleTemplate()).isEqualTo("实验室预约已取消");
	}

	@Test
	void deleteShouldRejectActiveSensorBinding() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(existingLab()));
		when(labSensorBindingService.existsActiveBindingByLabId(100L)).thenReturn(true);

		assertThatThrownBy(() -> labService.delete(100L))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_HAS_ACTIVE_SENSOR_BINDING.getCode());
		verify(labRepository, never()).softDelete(any());
	}

	@Test
	void updateStatusShouldReturnConflictWhenFutureReservationsNeedConfirmation() {
		Lab lab = existingLab();
		LabReservation reservation = futureReservation();
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.listFuturePendingOrApprovedByLabId(eq(100L), any(LocalDate.class)))
				.thenReturn(List.of(reservation));
		stubDetailLookups();

		LabStatusUpdateResponse response = labService.updateStatus(100L, LabStatusUpdateCommand.builder()
				.openStatus(LabOpenStatus.TEMPORARY_CLOSED.getCode())
				.changeReason("临时停电")
				.operatorId(99L)
				.build());

		assertThat(response.isApplied()).isFalse();
		assertThat(response.getConflictedReservations()).hasSize(1);
		assertThat(response.getMessage()).contains("确认");
		verify(labRepository, never()).updateStatus(any());
		verify(labReservationRepository, never()).updateBatch(any());
	}

	@Test
	void updateStatusShouldCancelFutureReservationsAndWriteLog() {
		Lab lab = existingLab();
		LabReservation reservation = futureReservation();
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.listFuturePendingOrApprovedByLabId(eq(100L), any(LocalDate.class)))
				.thenReturn(List.of(reservation));
		stubDetailLookups();

		LabStatusUpdateResponse response = labService.updateStatus(100L, LabStatusUpdateCommand.builder()
				.openStatus(LabOpenStatus.TEMPORARY_CLOSED.getCode())
				.changeReason("临时停电")
				.operatorId(99L)
				.forceCancelFutureReservations(true)
				.build());

		assertThat(response.isApplied()).isTrue();
		assertThat(response.getLab().getOpenStatus()).isEqualTo(LabOpenStatus.TEMPORARY_CLOSED.getCode());
		ArgumentCaptor<List<LabReservation>> reservationsCaptor = ArgumentCaptor.forClass(List.class);
		verify(labReservationRepository).updateBatch(reservationsCaptor.capture());
		assertThat(reservationsCaptor.getValue().getFirst().getReservationStatus()).isEqualTo(LabReservationStatus.CANCELLED);
		assertThat(reservationsCaptor.getValue().getFirst().getCancelReason()).contains("临时关闭");
		verify(labOccupancyCacheManager).evict(100L, reservation.getReservationDate());
		ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
		verify(labRepository).updateStatus(labCaptor.capture());
		assertThat(labCaptor.getValue().getRunningStatus()).isEqualTo(LabRunningStatus.IDLE);
		assertThat(labCaptor.getValue().getOpenStatus()).isEqualTo(LabOpenStatus.TEMPORARY_CLOSED);
		ArgumentCaptor<LabStatusChangeLog> logCaptor = ArgumentCaptor.forClass(LabStatusChangeLog.class);
		verify(labStatusChangeLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getCancelledReservationCount()).isEqualTo(1);
	}

	@Test
	void updateStatusShouldForceCheckoutWhenEnteringMaintenance() {
		Lab lab = existingLab();
		lab.setRunningStatus(LabRunningStatus.IN_USE);
		LabReservation reservation = checkedInReservation();
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lab));
		when(labReservationRepository.listActiveCheckedInByLabIdForUpdate(100L)).thenReturn(List.of(reservation));
		stubDetailLookups();

		LabStatusUpdateResponse response = labService.updateStatus(100L, LabStatusUpdateCommand.builder()
				.runningStatus(LabRunningStatus.MAINTENANCE.getCode())
				.changeReason("设备异常")
				.operatorId(99L)
				.forceFinishCurrentUsage(true)
				.build());

		assertThat(response.isApplied()).isTrue();
		assertThat(reservation.getCheckOutAt()).isNotNull();
		verify(labOccupancyCacheManager).evict(100L, reservation.getReservationDate());
		ArgumentCaptor<LabStatusChangeLog> logCaptor = ArgumentCaptor.forClass(LabStatusChangeLog.class);
		verify(labStatusChangeLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getForcedCheckoutCount()).isEqualTo(1);
		assertThat(logCaptor.getValue().getToRunningStatus()).isEqualTo(LabRunningStatus.MAINTENANCE);
	}

	@Test
	void updateStatusShouldRejectDirectSetInUse() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(existingLab()));

		assertThatThrownBy(() -> labService.updateStatus(100L, LabStatusUpdateCommand.builder()
				.runningStatus(LabRunningStatus.IN_USE.getCode())
				.changeReason("手动设置使用中")
				.operatorId(99L)
				.build()))
				.isInstanceOf(LabManagementException.class)
				.extracting("code")
				.isEqualTo(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID.getCode());
		verify(labRepository, never()).updateStatus(any());
	}

	private LabCreateCommand createCommand() {
		return LabCreateCommand.builder()
				.labName("AI实验室")
				.labTypeId(1L)
				.location(location())
				.capacity(null)
				.areaM2(BigDecimal.valueOf(80))
				.description("用于AI实验")
				.build();
	}

	private Lab existingLab() {
		return Lab.builder()
				.id(100L)
				.labName("机器人实验室")
				.labTypeId(1L)
				.location(location())
				.capacity(40)
				.areaM2(BigDecimal.valueOf(90))
				.runningStatus(LabRunningStatus.IDLE)
				.openStatus(LabOpenStatus.OPEN)
				.description("用于机器人实验")
				.createdAt(LocalDateTime.of(2026, 7, 8, 10, 0))
				.updatedAt(LocalDateTime.of(2026, 7, 8, 10, 0))
				.build();
	}

	private LabReservation futureReservation() {
		return LabReservation.builder()
				.id(200L)
				.reservationNo("LAB_RESV-202607100001")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(LocalDate.now().plusDays(1))
				.startTime(LocalTime.of(9, 0))
				.endTime(LocalTime.of(10, 0))
				.reservationStatus(LabReservationStatus.APPROVED)
				.build();
	}

	private LabReservation checkedInReservation() {
		return LabReservation.builder()
				.id(201L)
				.reservationNo("LAB_RESV-202607100002")
				.labId(100L)
				.applicantId(10L)
				.reservationDate(LocalDate.now())
				.startTime(LocalTime.now().minusMinutes(10))
				.endTime(LocalTime.now().plusMinutes(50))
				.reservationStatus(LabReservationStatus.CHECKED_IN)
				.checkInAt(LocalDateTime.now().minusMinutes(10))
				.build();
	}

	private void stubDetailLookups() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType()));
		when(labScheduleCacheManager.listByLabId(eq(100L), any())).thenReturn(List.of());
	}

	private LabLocation location() {
		return LabLocation.builder()
				.campus("南校区")
				.building("计算机楼")
				.floor(2)
				.roomNo("201")
				.build();
	}

	private LabType labType() {
		return LabType.builder()
				.id(1L)
				.labTypeName("人工智能实验室")
				.build();
	}

	private PersonDirectoryItemDTO person() {
		return PersonDirectoryItemDTO.builder()
				.personId(1L)
				.personName("陆喜言")
				.userId(10L)
				.build();
	}
}
