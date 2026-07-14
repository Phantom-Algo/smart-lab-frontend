package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseStudentDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.CourseStudentMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseStudentRepositoryImpl implements CourseStudentRepository {

	private final CourseStudentMapper courseStudentMapper;

	@Override
	public Optional<CourseStudentDO> findByCourseIdAndStudentId(Long courseId, Long studentId) {
		return Optional.ofNullable(courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudentDO>()
				.eq(CourseStudentDO::getCourseId, courseId)
				.eq(CourseStudentDO::getStudentId, studentId)
				.last("limit 1")));
	}

	@Override
	public List<CourseStudentDO> listByCourseId(Long courseId) {
		return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudentDO>()
				.eq(CourseStudentDO::getCourseId, courseId)
				.orderByAsc(CourseStudentDO::getId));
	}

	@Override
	public List<CourseStudentDO> listByCourseIds(Collection<Long> courseIds) {
		if (courseIds == null || courseIds.isEmpty()) {
			return List.of();
		}
		return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudentDO>()
				.in(CourseStudentDO::getCourseId, courseIds));
	}

	@Override
	public List<CourseStudentDO> listByStudentId(Long studentId) {
		return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudentDO>()
				.eq(CourseStudentDO::getStudentId, studentId)
				.orderByDesc(CourseStudentDO::getUpdatedAt, CourseStudentDO::getId));
	}

	@Override
	public void insert(CourseStudentDO courseStudentDO) {
		courseStudentMapper.insert(courseStudentDO);
	}

	@Override
	public void update(CourseStudentDO courseStudentDO) {
		courseStudentMapper.updateById(courseStudentDO);
	}
}
