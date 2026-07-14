package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseGradePublishDO;

import java.util.Optional;

public interface CourseGradePublishRepository {

	Optional<CourseGradePublishDO> findByCourseId(Long courseId);

	void insert(CourseGradePublishDO courseGradePublishDO);

	void update(CourseGradePublishDO courseGradePublishDO);
}
