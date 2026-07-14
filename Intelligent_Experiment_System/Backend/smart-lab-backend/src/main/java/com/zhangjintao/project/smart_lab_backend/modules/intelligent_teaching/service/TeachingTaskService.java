package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskPublishRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskPageItemResponse;

import java.util.List;

public interface TeachingTaskService {

	TeachingTaskDetailResponse create(TeachingTaskCreateRequest request);

	TeachingTaskDetailResponse update(Long id, TeachingTaskUpdateRequest request);

	TeachingTaskDetailResponse getById(Long id);

	PageResult<TeachingTaskPageItemResponse> page(TeachingTaskQueryRequest request);

	List<TeachingTaskPageItemResponse> listByCourseId(Long courseId);

	TeachingTaskDetailResponse publish(Long id, TeachingTaskPublishRequest request);

	void close(Long id);
}
