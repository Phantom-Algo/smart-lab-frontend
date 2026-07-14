package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabDirectoryServiceImpl implements LabDirectoryPort {

	private final LabRepository labRepository;

	@Override
	public Optional<LabDirectoryItemDTO> getById(Long labId) {
		return labRepository.findById(labId).map(this::toItem);
	}

	@Override
	public Map<Long, LabDirectoryItemDTO> getByIds(Collection<Long> labIds) {
		if (labIds == null || labIds.isEmpty()) {
			return Map.of();
		}
		return labRepository.mapByIdsIncludingDeleted(labIds).values().stream()
				.filter(lab -> lab.getDeletedAt() == null)
				.map(this::toItem)
				.collect(Collectors.toMap(LabDirectoryItemDTO::getLabId, Function.identity()));
	}

	private LabDirectoryItemDTO toItem(Lab lab) {
		LabLocation location = lab.getLocation();
		LabRunningStatus runningStatus = lab.getRunningStatus();
		LabOpenStatus openStatus = lab.getOpenStatus();
		return LabDirectoryItemDTO.builder()
				.labId(lab.getId())
				.labName(lab.getLabName())
				.labNo(buildLabNo(location))
				.campus(location == null ? null : location.getCampus())
				.building(location == null ? null : location.getBuilding())
				.floor(location == null ? null : location.getFloor())
				.roomNo(location == null ? null : location.getRoomNo())
				.capacity(lab.getCapacity())
				.runningStatus(runningStatus == null ? null : runningStatus.getCode())
				.runningStatusDesc(runningStatus == null ? null : runningStatus.getDescription())
				.openStatus(openStatus == null ? null : openStatus.getCode())
				.openStatusDesc(openStatus == null ? null : openStatus.getDescription())
				.available(isAvailable(runningStatus, openStatus))
				.build();
	}

	private String buildLabNo(LabLocation location) {
		if (location == null) {
			return null;
		}
		return String.join("-",
				valueOrPlaceholder(location.getCampus()),
				valueOrPlaceholder(location.getBuilding()),
				location.getFloor() == null ? "NA" : String.valueOf(location.getFloor()),
				valueOrPlaceholder(location.getRoomNo()));
	}

	private String valueOrPlaceholder(String value) {
		return (value == null || value.isBlank()) ? "NA" : value.trim();
	}

	private boolean isAvailable(LabRunningStatus runningStatus, LabOpenStatus openStatus) {
		return !Objects.equals(runningStatus, LabRunningStatus.DISABLED)
				&& !Objects.equals(runningStatus, LabRunningStatus.MAINTENANCE)
				&& Objects.equals(openStatus, LabOpenStatus.OPEN);
	}
}
