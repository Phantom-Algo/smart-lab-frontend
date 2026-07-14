package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseStudentDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseStudentRepository {

	Optional<CourseStudentDO> findByCourseIdAndStudentId(Long courseId, Long studentId);

	List<CourseStudentDO> listByCourseId(Long courseId);

	List<CourseStudentDO> listByCourseIds(Collection<Long> courseIds);

	List<CourseStudentDO> listByStudentId(Long studentId);

	void insert(CourseStudentDO courseStudentDO);

	void update(CourseStudentDO courseStudentDO);
}
