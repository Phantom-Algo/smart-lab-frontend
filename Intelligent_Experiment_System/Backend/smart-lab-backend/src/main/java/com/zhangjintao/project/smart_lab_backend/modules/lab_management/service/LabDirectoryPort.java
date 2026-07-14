package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabDirectoryItemDTO;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface LabDirectoryPort {

	Optional<LabDirectoryItemDTO> getById(Long labId);

	Map<Long, LabDirectoryItemDTO> getByIds(Collection<Long> labIds);
}
