package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {

	Optional<CourseDO> findById(Long id);

	List<CourseDO> listByIds(Collection<Long> ids);

	IPage<CourseDO> page(Long current, Long size, CourseQuery query);

	List<CourseDO> list(CourseQuery query);

	boolean existsByCourseCode(String courseCode, Long excludeId);

	boolean existsBySemesterAndCourseName(String semester, String courseName, Long excludeId);

	void insert(CourseDO courseDO);

	void update(CourseDO courseDO);
}
