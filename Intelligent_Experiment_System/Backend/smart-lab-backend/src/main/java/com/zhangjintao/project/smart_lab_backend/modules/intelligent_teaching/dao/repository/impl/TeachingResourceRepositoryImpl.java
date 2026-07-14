package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingResourceDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.TeachingResourceMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingResourceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingResourceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeachingResourceRepositoryImpl implements TeachingResourceRepository {

	private final TeachingResourceMapper teachingResourceMapper;

	@Override
	public Optional<TeachingResourceDO> findById(Long id) {
		return Optional.ofNullable(teachingResourceMapper.selectById(id));
	}

	@Override
	public IPage<TeachingResourceDO> page(Long current, Long size, TeachingResourceQuery query) {
		return teachingResourceMapper.selectPage(Page.of(current, size), buildQuery(query));
	}

	@Override
	public List<TeachingResourceDO> listByCourseId(Long courseId) {
		return teachingResourceMapper.selectList(new LambdaQueryWrapper<TeachingResourceDO>()
				.eq(TeachingResourceDO::getCourseId, courseId)
				.orderByAsc(TeachingResourceDO::getSortNo, TeachingResourceDO::getId));
	}

	@Override
	public void insert(TeachingResourceDO teachingResourceDO) {
		teachingResourceMapper.insert(teachingResourceDO);
	}

	@Override
	public void update(TeachingResourceDO teachingResourceDO) {
		teachingResourceMapper.updateById(teachingResourceDO);
	}

	@Override
	public void deleteById(Long id) {
		teachingResourceMapper.deleteById(id);
	}

	private LambdaQueryWrapper<TeachingResourceDO> buildQuery(TeachingResourceQuery query) {
		LambdaQueryWrapper<TeachingResourceDO> wrapper = new LambdaQueryWrapper<TeachingResourceDO>()
				.orderByAsc(TeachingResourceDO::getSortNo)
				.orderByDesc(TeachingResourceDO::getId);
		if (query == null) {
			return wrapper;
		}
		if (query.getCourseId() != null) {
			wrapper.eq(TeachingResourceDO::getCourseId, query.getCourseId());
		}
		if (query.getResourceType() != null) {
			wrapper.eq(TeachingResourceDO::getResourceType, query.getResourceType());
		}
		if (query.getUploadedBy() != null) {
			wrapper.eq(TeachingResourceDO::getUploadedBy, query.getUploadedBy());
		}
		if (StringUtils.hasText(query.getKeyword())) {
			wrapper.like(TeachingResourceDO::getResourceName, query.getKeyword().trim());
		}
		return wrapper;
	}
}
