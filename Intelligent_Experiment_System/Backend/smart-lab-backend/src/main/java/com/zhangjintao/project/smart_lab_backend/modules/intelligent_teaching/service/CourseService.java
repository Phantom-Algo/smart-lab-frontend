package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBatchEnrollRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CoursePageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;

import java.util.List;

public interface CourseService {

	CourseDetailResponse create(CourseCreateRequest request);

	CourseDetailResponse update(Long id, CourseUpdateRequest request);

	CourseDetailResponse getById(Long id);

	PageResult<CoursePageItemResponse> page(CourseQueryRequest request);

	void archive(Long id);

	void delete(Long id);

	List<CourseStudentResponse> listStudents(Long courseId);

	void batchEnroll(Long courseId, CourseBatchEnrollRequest request);

	void batchWithdraw(Long courseId, CourseBatchEnrollRequest request);
}
