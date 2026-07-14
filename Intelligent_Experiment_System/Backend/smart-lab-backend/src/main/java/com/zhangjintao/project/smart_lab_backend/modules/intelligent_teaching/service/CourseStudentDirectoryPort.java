package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;

import java.util.List;

public interface CourseStudentDirectoryPort {

	List<PersonDirectoryItemDTO> listStudentsByCourseId(Long courseId);

	List<CourseStudentResponse> listCourseStudentResponses(Long courseId);

	boolean isCourseStudent(Long courseId, Long studentId);

	boolean isCourseTeacher(Long courseId, Long teacherId);
}
