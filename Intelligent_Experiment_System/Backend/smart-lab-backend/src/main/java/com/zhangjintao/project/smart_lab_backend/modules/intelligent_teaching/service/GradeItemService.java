package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.GradeItemResponse;

import java.util.List;

public interface GradeItemService {

	GradeItemResponse create(Long courseId, GradeItemCreateRequest request);

	GradeItemResponse update(Long id, GradeItemUpdateRequest request);

	void delete(Long id);

	List<GradeItemResponse> listByCourseId(Long courseId);

	void reorder(Long courseId, GradeItemReorderRequest request);
}
