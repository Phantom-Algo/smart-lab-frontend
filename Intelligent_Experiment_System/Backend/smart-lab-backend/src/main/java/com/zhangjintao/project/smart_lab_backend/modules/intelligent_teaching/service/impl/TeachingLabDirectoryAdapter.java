package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.LabSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.LabDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingLabDirectoryAdapter implements LabDirectoryPort {

	private final com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabDirectoryPort labDirectoryPort;

	@Override
	public Optional<LabSummaryDTO> getByLabId(Long labId) {
		return labDirectoryPort.getById(labId).map(this::toSummary);
	}

	@Override
	public Map<Long, LabSummaryDTO> getByLabIds(Collection<Long> labIds) {
		return labDirectoryPort.getByIds(labIds).values().stream()
				.map(this::toSummary)
				.collect(Collectors.toMap(LabSummaryDTO::getLabId, Function.identity()));
	}

	private LabSummaryDTO toSummary(
			com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabDirectoryItemDTO item) {
		return LabSummaryDTO.builder()
				.labId(item.getLabId())
				.labName(item.getLabName())
				.labNo(item.getLabNo())
				.campus(item.getCampus())
				.building(item.getBuilding())
				.floor(item.getFloor())
				.roomNo(item.getRoomNo())
				.capacity(item.getCapacity())
				.runningStatus(item.getRunningStatus())
				.runningStatusDesc(item.getRunningStatusDesc())
				.openStatus(item.getOpenStatus())
				.openStatusDesc(item.getOpenStatusDesc())
				.available(item.getAvailable())
				.build();
	}
}
