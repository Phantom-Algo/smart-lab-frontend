package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.mapper.PositionMapper;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PositionRepositoryImpl implements PositionRepository {

	private final PositionMapper positionMapper;

	@Override
	public Optional<PositionDO> findById(Long id) {
		return Optional.ofNullable(positionMapper.selectOne(new LambdaQueryWrapper<PositionDO>()
				.eq(PositionDO::getId, id)
				.isNull(PositionDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public IPage<PositionDO> page(Long current, Long size, Long departmentId, Integer positionStatus) {
		LambdaQueryWrapper<PositionDO> wrapper = new LambdaQueryWrapper<PositionDO>()
				.isNull(PositionDO::getDeletedAt)
				.orderByDesc(PositionDO::getUpdatedAt, PositionDO::getId);
		if (departmentId != null) {
			wrapper.eq(PositionDO::getDepartmentId, departmentId);
		}
		if (positionStatus != null) {
			wrapper.eq(PositionDO::getPositionStatus, positionStatus);
		}
		return positionMapper.selectPage(Page.of(current, size), wrapper);
	}

	@Override
	public List<PositionDO> listByDepartmentId(Long departmentId) {
		return positionMapper.selectList(new LambdaQueryWrapper<PositionDO>()
				.eq(PositionDO::getDepartmentId, departmentId)
				.isNull(PositionDO::getDeletedAt)
				.orderByAsc(PositionDO::getSortNo, PositionDO::getId));
	}

	@Override
	public boolean existsByPositionNo(String positionNo, Long excludeId) {
		LambdaQueryWrapper<PositionDO> wrapper = new LambdaQueryWrapper<PositionDO>()
				.eq(PositionDO::getPositionNo, positionNo)
				.isNull(PositionDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(PositionDO::getId, excludeId);
		}
		return positionMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByPositionCode(String positionCode, Long excludeId) {
		LambdaQueryWrapper<PositionDO> wrapper = new LambdaQueryWrapper<PositionDO>()
				.eq(PositionDO::getPositionCode, positionCode)
				.isNull(PositionDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(PositionDO::getId, excludeId);
		}
		return positionMapper.selectCount(wrapper) > 0;
	}

	@Override
	public long countByDepartmentId(Long departmentId) {
		return positionMapper.selectCount(new LambdaQueryWrapper<PositionDO>()
				.eq(PositionDO::getDepartmentId, departmentId)
				.isNull(PositionDO::getDeletedAt));
	}

	@Override
	public void insert(PositionDO positionDO) {
		positionMapper.insert(positionDO);
	}

	@Override
	public void update(PositionDO positionDO) {
		positionMapper.updateById(positionDO);
	}
}
