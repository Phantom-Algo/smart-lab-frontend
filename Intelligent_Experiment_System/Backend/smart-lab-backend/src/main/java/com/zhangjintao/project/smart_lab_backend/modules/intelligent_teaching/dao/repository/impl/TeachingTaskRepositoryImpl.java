package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.TeachingTaskMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingTaskRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeachingTaskRepositoryImpl implements TeachingTaskRepository {

	private final TeachingTaskMapper teachingTaskMapper;

	@Override
	public Optional<TeachingTaskDO> findById(Long id) {
		return Optional.ofNullable(teachingTaskMapper.selectOne(new LambdaQueryWrapper<TeachingTaskDO>()
				.eq(TeachingTaskDO::getId, id)
				.isNull(TeachingTaskDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public IPage<TeachingTaskDO> page(Long current, Long size, TeachingTaskQuery query) {
		return teachingTaskMapper.selectPage(Page.of(current, size), buildQuery(query));
	}

	@Override
	public List<TeachingTaskDO> listByCourseId(Long courseId) {
		return teachingTaskMapper.selectList(new LambdaQueryWrapper<TeachingTaskDO>()
				.eq(TeachingTaskDO::getCourseId, courseId)
				.isNull(TeachingTaskDO::getDeletedAt)
				.orderByDesc(TeachingTaskDO::getUpdatedAt, TeachingTaskDO::getId));
	}

	@Override
	public void insert(TeachingTaskDO teachingTaskDO) {
		teachingTaskMapper.insert(teachingTaskDO);
	}

	@Override
	public void update(TeachingTaskDO teachingTaskDO) {
		teachingTaskMapper.updateById(teachingTaskDO);
	}

	private LambdaQueryWrapper<TeachingTaskDO> buildQuery(TeachingTaskQuery query) {
		LambdaQueryWrapper<TeachingTaskDO> wrapper = new LambdaQueryWrapper<TeachingTaskDO>()
				.isNull(TeachingTaskDO::getDeletedAt)
				.orderByDesc(TeachingTaskDO::getUpdatedAt, TeachingTaskDO::getId);
		if (query == null) {
			return wrapper;
		}
		if (query.getCourseId() != null) {
			wrapper.eq(TeachingTaskDO::getCourseId, query.getCourseId());
		}
		if (query.getCreatedBy() != null) {
			wrapper.eq(TeachingTaskDO::getCreatedBy, query.getCreatedBy());
		}
		if (query.getTaskStatus() != null) {
			wrapper.eq(TeachingTaskDO::getTaskStatus, query.getTaskStatus());
		}
		if (StringUtils.hasText(query.getKeyword())) {
			String keyword = query.getKeyword().trim();
			wrapper.and(item -> item.like(TeachingTaskDO::getTaskTitle, keyword)
					.or().like(TeachingTaskDO::getTaskNo, keyword));
		}
		return wrapper;
	}
}
