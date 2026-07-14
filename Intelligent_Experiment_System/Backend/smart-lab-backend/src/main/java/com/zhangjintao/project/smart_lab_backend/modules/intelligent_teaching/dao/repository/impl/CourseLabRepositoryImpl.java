package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseLabDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.CourseLabMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseLabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseLabRepositoryImpl implements CourseLabRepository {

	private final CourseLabMapper courseLabMapper;

	@Override
	public Optional<CourseLabDO> findByCourseIdAndLabId(Long courseId, Long labId) {
		return Optional.ofNullable(courseLabMapper.selectOne(new LambdaQueryWrapper<CourseLabDO>()
				.eq(CourseLabDO::getCourseId, courseId)
				.eq(CourseLabDO::getLabId, labId)
				.last("limit 1")));
	}

	@Override
	public List<CourseLabDO> listByCourseId(Long courseId) {
		return courseLabMapper.selectList(new LambdaQueryWrapper<CourseLabDO>()
				.eq(CourseLabDO::getCourseId, courseId)
				.orderByDesc(CourseLabDO::getUpdatedAt, CourseLabDO::getId));
	}

	@Override
	public void insert(CourseLabDO courseLabDO) {
		courseLabMapper.insert(courseLabDO);
	}

	@Override
	public void update(CourseLabDO courseLabDO) {
		courseLabMapper.updateById(courseLabDO);
	}
}
