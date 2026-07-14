package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.LabSummaryDTO;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface LabDirectoryPort {

	Optional<LabSummaryDTO> getByLabId(Long labId);

	Map<Long, LabSummaryDTO> getByLabIds(Collection<Long> labIds);
}
