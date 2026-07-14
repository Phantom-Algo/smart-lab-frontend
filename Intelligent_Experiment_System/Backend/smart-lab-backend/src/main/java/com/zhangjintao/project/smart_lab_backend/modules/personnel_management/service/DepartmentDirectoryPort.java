package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.DepartmentSummaryDTO;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface DepartmentDirectoryPort {

	Optional<DepartmentSummaryDTO> getById(Long departmentId);

	Map<Long, DepartmentSummaryDTO> getByIds(Collection<Long> departmentIds);
}
