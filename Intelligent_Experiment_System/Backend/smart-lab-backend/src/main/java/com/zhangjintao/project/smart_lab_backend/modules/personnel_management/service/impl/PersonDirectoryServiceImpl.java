package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AccountBindingDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.PersonAccountPort;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.DepartmentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PositionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter.PersonnelManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryQuery;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
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
public class PersonDirectoryServiceImpl implements PersonDirectoryPort {

	private final PersonRepository personRepository;
	private final DepartmentRepository departmentRepository;
	private final PositionRepository positionRepository;
	private final PersonAccountPort personAccountPort;

	@Override
	public Optional<PersonDirectoryItemDTO> getByPersonId(Long personId) {
		return personRepository.findById(personId).map(personDO -> compose(List.of(personDO)).stream().findFirst().orElseThrow());
	}

	@Override
	public Optional<PersonDirectoryItemDTO> getByUserId(Long userId) {
		Optional<AccountBindingDTO> bindingOptional = personAccountPort.getAccountBindingByUserId(userId);
		if (bindingOptional.isEmpty()) {
			return Optional.empty();
		}
		return getByPersonId(bindingOptional.get().getPersonId());
	}

	@Override
	public List<PersonDirectoryItemDTO> listByPersonIds(Collection<Long> personIds) {
		return compose(personRepository.listByIds(personIds));
	}

	@Override
	public PageResult<PersonDirectoryItemDTO> page(PersonDirectoryQuery query) {
		PersonDirectoryQuery safeQuery = query == null ? PersonDirectoryQuery.builder().build() : query;
		long pageNum = safeQuery.getPageNum() == null ? 1L : safeQuery.getPageNum();
		long pageSize = safeQuery.getPageSize() == null ? 10L : safeQuery.getPageSize();
		List<PersonDirectoryItemDTO> allRecords = list(safeQuery);
		int fromIndex = (int) Math.min((pageNum - 1) * pageSize, allRecords.size());
		int toIndex = (int) Math.min(fromIndex + pageSize, allRecords.size());
		return PageResult.of(allRecords.subList(fromIndex, toIndex), allRecords.size(), pageNum, pageSize);
	}

	@Override
	public List<PersonDirectoryItemDTO> list(PersonDirectoryQuery query) {
		PersonDirectoryQuery safeQuery = query == null ? PersonDirectoryQuery.builder().build() : query;
		return applyAccountFilter(compose(personRepository.list(safeQuery)), safeQuery.getHasAccount());
	}

	private List<PersonDirectoryItemDTO> compose(List<PersonDO> people) {
		if (people.isEmpty()) {
			return List.of();
		}
		Map<Long, DepartmentDO> departmentMap = departmentRepository.listAll().stream()
				.collect(Collectors.toMap(DepartmentDO::getId, Function.identity()));
		Map<Long, PositionDO> positionMap = people.stream()
				.map(PersonDO::getPositionId)
				.filter(id -> id != null)
				.distinct()
				.map(positionRepository::findById)
				.flatMap(Optional::stream)
				.collect(Collectors.toMap(PositionDO::getId, Function.identity()));
		Map<Long, AccountBindingDTO> accountMap = personAccountPort.getAccountBindingsByPersonIds(
				people.stream().map(PersonDO::getId).toList());
		return people.stream()
				.map(personDO -> PersonnelManagementConverter.toPersonDirectoryItemDTO(personDO,
						departmentMap.get(personDO.getDepartmentId()),
						positionMap.get(personDO.getPositionId()),
						accountMap.get(personDO.getId())))
				.toList();
	}

	private List<PersonDirectoryItemDTO> applyAccountFilter(List<PersonDirectoryItemDTO> items, Boolean hasAccount) {
		if (hasAccount == null) {
			return items;
		}
		return items.stream()
				.filter(item -> hasAccount ? item.getUserId() != null : item.getUserId() == null)
				.toList();
	}
}
