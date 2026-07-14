package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeItemDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.GradeItemMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.GradeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GradeItemRepositoryImpl implements GradeItemRepository {

	private final GradeItemMapper gradeItemMapper;

	@Override
	public Optional<GradeItemDO> findById(Long id) {
		return Optional.ofNullable(gradeItemMapper.selectById(id));
	}

	@Override
	public List<GradeItemDO> listByCourseId(Long courseId) {
		return gradeItemMapper.selectList(new LambdaQueryWrapper<GradeItemDO>()
				.eq(GradeItemDO::getCourseId, courseId)
				.orderByAsc(GradeItemDO::getSortNo, GradeItemDO::getId));
	}

	@Override
	public boolean existsByCourseIdAndItemName(Long courseId, String itemName, Long excludeId) {
		if (!StringUtils.hasText(itemName)) {
			return false;
		}
		LambdaQueryWrapper<GradeItemDO> wrapper = new LambdaQueryWrapper<GradeItemDO>()
				.eq(GradeItemDO::getCourseId, courseId)
				.eq(GradeItemDO::getItemName, itemName);
		if (excludeId != null) {
			wrapper.ne(GradeItemDO::getId, excludeId);
		}
		return gradeItemMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByRelatedTaskId(Long relatedTaskId, Long excludeId) {
		if (relatedTaskId == null) {
			return false;
		}
		LambdaQueryWrapper<GradeItemDO> wrapper = new LambdaQueryWrapper<GradeItemDO>()
				.eq(GradeItemDO::getRelatedTaskId, relatedTaskId);
		if (excludeId != null) {
			wrapper.ne(GradeItemDO::getId, excludeId);
		}
		return gradeItemMapper.selectCount(wrapper) > 0;
	}

	@Override
	public BigDecimal sumWeightByCourseId(Long courseId, Long excludeId, BigDecimal replacingWeight) {
		BigDecimal sum = listByCourseId(courseId).stream()
				.filter(item -> excludeId == null || !excludeId.equals(item.getId()))
				.map(GradeItemDO::getItemWeight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return replacingWeight == null ? sum : sum.add(replacingWeight);
	}

	@Override
	public void insert(GradeItemDO gradeItemDO) {
		gradeItemMapper.insert(gradeItemDO);
	}

	@Override
	public void update(GradeItemDO gradeItemDO) {
		gradeItemMapper.updateById(gradeItemDO);
	}

	@Override
	public void deleteById(Long id) {
		gradeItemMapper.deleteById(id);
	}
}
