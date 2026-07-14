package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.mapper.PersonMapper;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonRepositoryImpl implements PersonRepository {

	private final PersonMapper personMapper;

	@Override
	public Optional<PersonDO> findById(Long id) {
		return Optional.ofNullable(personMapper.selectOne(new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getId, id)
				.isNull(PersonDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public List<PersonDO> listByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return personMapper.selectList(new LambdaQueryWrapper<PersonDO>()
				.in(PersonDO::getId, ids)
				.isNull(PersonDO::getDeletedAt));
	}

	@Override
	public IPage<PersonDO> page(Long current, Long size, PersonDirectoryQuery query) {
		return personMapper.selectPage(Page.of(current, size), buildQuery(query));
	}

	@Override
	public List<PersonDO> list(PersonDirectoryQuery query) {
		return personMapper.selectList(buildQuery(query));
	}

	@Override
	public boolean existsByPersonNo(String personNo, Long excludeId) {
		if (!StringUtils.hasText(personNo)) {
			return false;
		}
		LambdaQueryWrapper<PersonDO> wrapper = new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getPersonNo, personNo)
				.isNull(PersonDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(PersonDO::getId, excludeId);
		}
		return personMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByStudentNo(String studentNo, Long excludeId) {
		if (!StringUtils.hasText(studentNo)) {
			return false;
		}
		LambdaQueryWrapper<PersonDO> wrapper = new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getStudentNo, studentNo)
				.isNull(PersonDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(PersonDO::getId, excludeId);
		}
		return personMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByEmployeeNo(String employeeNo, Long excludeId) {
		if (!StringUtils.hasText(employeeNo)) {
			return false;
		}
		LambdaQueryWrapper<PersonDO> wrapper = new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getEmployeeNo, employeeNo)
				.isNull(PersonDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(PersonDO::getId, excludeId);
		}
		return personMapper.selectCount(wrapper) > 0;
	}

	@Override
	public long countByDepartmentId(Long departmentId) {
		return personMapper.selectCount(new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getDepartmentId, departmentId)
				.isNull(PersonDO::getDeletedAt));
	}

	@Override
	public long countByPositionId(Long positionId) {
		return personMapper.selectCount(new LambdaQueryWrapper<PersonDO>()
				.eq(PersonDO::getPositionId, positionId)
				.isNull(PersonDO::getDeletedAt));
	}

	@Override
	public void insert(PersonDO personDO) {
		personMapper.insert(personDO);
	}

	@Override
	public void update(PersonDO personDO) {
		personMapper.updateById(personDO);
	}

	private LambdaQueryWrapper<PersonDO> buildQuery(PersonDirectoryQuery query) {
		LambdaQueryWrapper<PersonDO> wrapper = new LambdaQueryWrapper<PersonDO>()
				.isNull(PersonDO::getDeletedAt)
				.orderByDesc(PersonDO::getUpdatedAt, PersonDO::getId);
		if (query == null) {
			return wrapper;
		}
		if (query.getDepartmentId() != null) {
			wrapper.eq(PersonDO::getDepartmentId, query.getDepartmentId());
		}
		if (query.getPositionId() != null) {
			wrapper.eq(PersonDO::getPositionId, query.getPositionId());
		}
		if (query.getPersonTypes() != null && !query.getPersonTypes().isEmpty()) {
			wrapper.in(PersonDO::getPersonType, query.getPersonTypes());
		}
		if (query.getPersonStatuses() != null && !query.getPersonStatuses().isEmpty()) {
			wrapper.in(PersonDO::getPersonStatus, query.getPersonStatuses());
		}
		if (StringUtils.hasText(query.getKeyword())) {
			String keyword = query.getKeyword().trim();
			wrapper.and(item -> item.like(PersonDO::getPersonName, keyword)
					.or().like(PersonDO::getStudentNo, keyword)
					.or().like(PersonDO::getEmployeeNo, keyword));
		}
		return wrapper;
	}
}
