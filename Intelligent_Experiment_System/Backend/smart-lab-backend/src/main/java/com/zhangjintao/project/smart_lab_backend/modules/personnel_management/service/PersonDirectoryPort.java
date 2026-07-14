package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonDirectoryPort {

	Optional<PersonDirectoryItemDTO> getByPersonId(Long personId);

	Optional<PersonDirectoryItemDTO> getByUserId(Long userId);

	List<PersonDirectoryItemDTO> listByPersonIds(Collection<Long> personIds);

	PageResult<PersonDirectoryItemDTO> page(PersonDirectoryQuery query);

	List<PersonDirectoryItemDTO> list(PersonDirectoryQuery query);
}
