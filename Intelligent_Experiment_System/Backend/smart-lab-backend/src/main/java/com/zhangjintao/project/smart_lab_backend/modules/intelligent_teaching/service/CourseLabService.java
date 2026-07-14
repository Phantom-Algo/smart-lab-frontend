package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBindLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseLabBindingResponse;

import java.util.List;

public interface CourseLabService {

	List<CourseLabBindingResponse> listByCourseId(Long courseId);

	CourseLabBindingResponse bind(Long courseId, CourseBindLabRequest request);

	void unbind(Long courseId, Long labId);
}
