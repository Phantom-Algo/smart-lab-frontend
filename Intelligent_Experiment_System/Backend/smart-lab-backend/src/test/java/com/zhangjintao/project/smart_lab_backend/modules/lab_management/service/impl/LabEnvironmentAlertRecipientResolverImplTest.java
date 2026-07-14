package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageRecipientUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabEnvironmentAlertRecipientResolverImplTest {

	@Mock
	private MessageRecipientUserPort messageRecipientUserPort;

	@Mock
	private LabReservationRepository labReservationRepository;

	@InjectMocks
	private LabEnvironmentAlertRecipientResolverImpl resolver;

	@Test
	void resolveRecipientsShouldIncludeManagersDutyUserAndCurrentApplicantsWithoutDuplicates() {
		LocalDateTime detectedAt = LocalDateTime.of(2026, 7, 10, 10, 30);
		Lab lab = Lab.builder()
				.id(100L)
				.dutyUserId(2L)
				.runningStatus(LabRunningStatus.IN_USE)
				.build();
		when(messageRecipientUserPort.listManagerUserIds()).thenReturn(List.of(1L, 2L));
		when(labReservationRepository.listCurrentCheckedInByLabId(100L, detectedAt))
				.thenReturn(List.of(LabReservation.builder().applicantId(3L).build(),
						LabReservation.builder().applicantId(1L).build()));

		List<Long> recipients = resolver.resolveRecipients(LabSensorDataConsumeResult.builder()
				.lab(lab)
				.detectedAt(detectedAt)
				.build());

		assertThat(recipients).containsExactly(1L, 2L, 3L);
	}

	@Test
	void resolveRecipientsShouldNotQueryReservationWhenLabNotInUse() {
		LocalDateTime detectedAt = LocalDateTime.of(2026, 7, 10, 10, 30);
		Lab lab = Lab.builder()
				.id(100L)
				.dutyUserId(2L)
				.runningStatus(LabRunningStatus.IDLE)
				.build();
		when(messageRecipientUserPort.listManagerUserIds()).thenReturn(List.of(1L));

		List<Long> recipients = resolver.resolveRecipients(LabSensorDataConsumeResult.builder()
				.lab(lab)
				.detectedAt(detectedAt)
				.build());

		assertThat(recipients).containsExactly(1L, 2L);
		verify(labReservationRepository, never()).listCurrentCheckedInByLabId(100L, detectedAt);
	}
}
