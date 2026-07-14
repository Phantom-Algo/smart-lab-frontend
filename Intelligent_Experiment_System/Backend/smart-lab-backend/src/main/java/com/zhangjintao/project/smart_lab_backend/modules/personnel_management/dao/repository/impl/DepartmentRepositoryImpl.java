package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.mapper.DepartmentMapper;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepository {

	private final DepartmentMapper departmentMapper;

	@Override
	public Optional<DepartmentDO> findById(Long id) {
		return Optional.ofNullable(departmentMapper.selectOne(new LambdaQueryWrapper<DepartmentDO>()
				.eq(DepartmentDO::getId, id)
				.isNull(DepartmentDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public List<DepartmentDO> listAll() {
		return departmentMapper.selectList(new LambdaQueryWrapper<DepartmentDO>()
				.isNull(DepartmentDO::getDeletedAt)
				.orderByAsc(DepartmentDO::getSortNo, DepartmentDO::getId));
	}

	@Override
	public List<DepartmentDO> listByParentId(Long parentId) {
		LambdaQueryWrapper<DepartmentDO> wrapper = new LambdaQueryWrapper<DepartmentDO>()
				.isNull(DepartmentDO::getDeletedAt)
				.orderByAsc(DepartmentDO::getSortNo, DepartmentDO::getId);
		if (parentId == null) {
			wrapper.isNull(DepartmentDO::getParentId);
		} else {
			wrapper.eq(DepartmentDO::getParentId, parentId);
		}
		return departmentMapper.selectList(wrapper);
	}

	@Override
	public boolean existsByDepartmentNo(String departmentNo, Long excludeId) {
		LambdaQueryWrapper<DepartmentDO> wrapper = new LambdaQueryWrapper<DepartmentDO>()
				.eq(DepartmentDO::getDepartmentNo, departmentNo)
				.isNull(DepartmentDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(DepartmentDO::getId, excludeId);
		}
		return departmentMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByDepartmentCode(String departmentCode, Long excludeId) {
		LambdaQueryWrapper<DepartmentDO> wrapper = new LambdaQueryWrapper<DepartmentDO>()
				.eq(DepartmentDO::getDepartmentCode, departmentCode)
				.isNull(DepartmentDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(DepartmentDO::getId, excludeId);
		}
		return departmentMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean hasChildren(Long id) {
		return departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentDO>()
				.eq(DepartmentDO::getParentId, id)
				.isNull(DepartmentDO::getDeletedAt)) > 0;
	}

	@Override
	public void insert(DepartmentDO departmentDO) {
		departmentMapper.insert(departmentDO);
	}

	@Override
	public void update(DepartmentDO departmentDO) {
		departmentMapper.updateById(departmentDO);
	}
}
