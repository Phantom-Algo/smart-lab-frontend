package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface LabRepository {

	Optional<Lab> findById(Long id);

	Optional<Lab> findByIdForUpdate(Long id);

	Map<Long, Lab> mapByIdsIncludingDeleted(Collection<Long> ids);

	Lab save(Lab lab);

	Lab update(Lab lab);

	void updateStatus(Lab lab);

	void softDelete(Lab lab);

	PageResult<LabPageItemDTO> page(LabQueryCriteria criteria);

	boolean existsByLocation(LabLocation location, Long excludeId);

	boolean existsByLabTypeId(Long labTypeId);
}
