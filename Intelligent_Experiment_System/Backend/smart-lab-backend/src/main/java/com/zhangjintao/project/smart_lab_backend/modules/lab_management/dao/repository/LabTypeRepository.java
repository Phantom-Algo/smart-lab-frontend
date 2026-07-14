package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LabTypeRepository {

	Optional<LabType> findById(Long id);

	List<LabType> listAll();

	Map<Long, LabType> mapByIds(Collection<Long> ids);

	boolean existsByName(String labTypeName, Long excludeId);

	LabType save(LabType labType);

	LabType update(LabType labType);

	void deleteById(Long id);
}
