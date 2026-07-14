package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseGradeItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseGradeStatsDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.StudentGradeSheetDTO;

import java.util.List;
import java.util.Optional;

public interface GradeDirectoryPort {

	List<CourseGradeItemDTO> listItemsByCourseId(Long courseId);

	List<StudentGradeSheetDTO> listStudentGradesByCourseId(Long courseId);

	Optional<StudentGradeSheetDTO> getStudentGradeSheet(Long courseId, Long studentId);

	CourseGradeStatsDTO getCourseGradeStatsSummary(Long courseId);
}
