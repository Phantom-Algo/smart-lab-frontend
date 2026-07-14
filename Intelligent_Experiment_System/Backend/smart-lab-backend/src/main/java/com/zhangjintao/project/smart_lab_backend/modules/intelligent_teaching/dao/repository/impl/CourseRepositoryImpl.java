package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.CourseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

	private final CourseMapper courseMapper;

	@Override
	public Optional<CourseDO> findById(Long id) {
		return Optional.ofNullable(courseMapper.selectOne(new LambdaQueryWrapper<CourseDO>()
				.eq(CourseDO::getId, id)
				.isNull(CourseDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public List<CourseDO> listByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return courseMapper.selectList(new LambdaQueryWrapper<CourseDO>()
				.in(CourseDO::getId, ids)
				.isNull(CourseDO::getDeletedAt));
	}

	@Override
	public IPage<CourseDO> page(Long current, Long size, CourseQuery query) {
		return courseMapper.selectPage(Page.of(current, size), buildQuery(query));
	}

	@Override
	public List<CourseDO> list(CourseQuery query) {
		return courseMapper.selectList(buildQuery(query));
	}

	@Override
	public boolean existsByCourseCode(String courseCode, Long excludeId) {
		LambdaQueryWrapper<CourseDO> wrapper = new LambdaQueryWrapper<CourseDO>()
				.eq(CourseDO::getCourseCode, courseCode)
				.isNull(CourseDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(CourseDO::getId, excludeId);
		}
		return courseMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsBySemesterAndCourseName(String semester, String courseName, Long excludeId) {
		LambdaQueryWrapper<CourseDO> wrapper = new LambdaQueryWrapper<CourseDO>()
				.eq(CourseDO::getSemester, semester)
				.eq(CourseDO::getCourseName, courseName)
				.isNull(CourseDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(CourseDO::getId, excludeId);
		}
		return courseMapper.selectCount(wrapper) > 0;
	}

	@Override
	public void insert(CourseDO courseDO) {
		courseMapper.insert(courseDO);
	}

	@Override
	public void update(CourseDO courseDO) {
		courseMapper.updateById(courseDO);
	}

	private LambdaQueryWrapper<CourseDO> buildQuery(CourseQuery query) {
		LambdaQueryWrapper<CourseDO> wrapper = new LambdaQueryWrapper<CourseDO>()
				.isNull(CourseDO::getDeletedAt)
				.orderByDesc(CourseDO::getUpdatedAt, CourseDO::getId);
		if (query == null) {
			return wrapper;
		}
		if (StringUtils.hasText(query.getKeyword())) {
			String keyword = query.getKeyword().trim();
			wrapper.and(item -> item.like(CourseDO::getCourseName, keyword)
					.or().like(CourseDO::getCourseCode, keyword));
		}
		if (StringUtils.hasText(query.getSemester())) {
			wrapper.eq(CourseDO::getSemester, query.getSemester());
		}
		if (query.getTeacherId() != null) {
			wrapper.eq(CourseDO::getTeacherId, query.getTeacherId());
		}
		if (query.getDepartmentId() != null) {
			wrapper.eq(CourseDO::getDepartmentId, query.getDepartmentId());
		}
		if (query.getCourseStatus() != null) {
			wrapper.eq(CourseDO::getCourseStatus, query.getCourseStatus());
		}
		return wrapper;
	}
}
