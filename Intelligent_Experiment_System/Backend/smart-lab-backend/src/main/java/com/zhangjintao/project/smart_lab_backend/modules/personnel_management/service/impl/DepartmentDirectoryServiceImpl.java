package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.DepartmentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.DepartmentSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.DepartmentDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentDirectoryServiceImpl implements DepartmentDirectoryPort {

	private final DepartmentRepository departmentRepository;

	@Override
	public Optional<DepartmentSummaryDTO> getById(Long departmentId) {
		return departmentRepository.findById(departmentId).map(this::toSummary);
	}

	@Override
	public Map<Long, DepartmentSummaryDTO> getByIds(Collection<Long> departmentIds) {
		if (departmentIds == null || departmentIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = departmentIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
		return departmentRepository.listAll().stream()
				.filter(item -> ids.contains(item.getId()))
				.map(this::toSummary)
				.collect(Collectors.toMap(DepartmentSummaryDTO::getDepartmentId, Function.identity()));
	}

	private DepartmentSummaryDTO toSummary(DepartmentDO departmentDO) {
		return DepartmentSummaryDTO.builder()
				.departmentId(departmentDO.getId())
				.departmentNo(departmentDO.getDepartmentNo())
				.departmentCode(departmentDO.getDepartmentCode())
				.departmentName(departmentDO.getDepartmentName())
				.departmentStatus(departmentDO.getDepartmentStatus())
				.build();
	}
}
