package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseLabDO;

import java.util.List;
import java.util.Optional;

public interface CourseLabRepository {

	Optional<CourseLabDO> findByCourseIdAndLabId(Long courseId, Long labId);

	List<CourseLabDO> listByCourseId(Long courseId);

	void insert(CourseLabDO courseLabDO);

	void update(CourseLabDO courseLabDO);
}
