package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseGradePublishDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.CourseGradePublishMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseGradePublishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseGradePublishRepositoryImpl implements CourseGradePublishRepository {

	private final CourseGradePublishMapper courseGradePublishMapper;

	@Override
	public Optional<CourseGradePublishDO> findByCourseId(Long courseId) {
		return Optional.ofNullable(courseGradePublishMapper.selectOne(new LambdaQueryWrapper<CourseGradePublishDO>()
				.eq(CourseGradePublishDO::getCourseId, courseId)
				.last("limit 1")));
	}

	@Override
	public void insert(CourseGradePublishDO courseGradePublishDO) {
		courseGradePublishMapper.insert(courseGradePublishDO);
	}

	@Override
	public void update(CourseGradePublishDO courseGradePublishDO) {
		courseGradePublishMapper.updateById(courseGradePublishDO);
	}
}
