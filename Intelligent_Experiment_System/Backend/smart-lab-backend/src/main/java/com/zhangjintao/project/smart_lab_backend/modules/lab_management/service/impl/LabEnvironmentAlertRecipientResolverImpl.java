package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabEnvironmentAlertRecipientResolver;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageRecipientUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LabEnvironmentAlertRecipientResolverImpl implements LabEnvironmentAlertRecipientResolver {

	private final MessageRecipientUserPort messageRecipientUserPort;
	private final LabReservationRepository labReservationRepository;

	@Override
	public List<Long> resolveRecipients(LabSensorDataConsumeResult result) {
		Set<Long> recipientUserIds = new LinkedHashSet<>(messageRecipientUserPort.listManagerUserIds());
		Lab lab = result.getLab();
		if (lab.getDutyUserId() != null) {
			recipientUserIds.add(lab.getDutyUserId());
		}
		if (lab.getRunningStatus() == LabRunningStatus.IN_USE) {
			labReservationRepository.listCurrentCheckedInByLabId(lab.getId(), result.getDetectedAt())
					.stream()
					.map(reservation -> reservation.getApplicantId())
					.forEach(recipientUserIds::add);
		}
		return recipientUserIds.stream().toList();
	}
}
