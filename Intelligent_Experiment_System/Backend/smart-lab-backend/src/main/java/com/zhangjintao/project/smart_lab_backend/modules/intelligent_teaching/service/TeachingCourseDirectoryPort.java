package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseQuery;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseSummaryDTO;

import java.util.List;
import java.util.Optional;

public interface TeachingCourseDirectoryPort {

	Optional<CourseSummaryDTO> getByCourseId(Long courseId);

	PageResult<CourseSummaryDTO> page(CourseQuery query);

	List<CourseSummaryDTO> listByTeacherId(Long teacherId);

	List<CourseSummaryDTO> listByStudentId(Long studentId);
}
