package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.ResourceUploadPrepareRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.ResourceUploadPrepareResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingResourceResponse;

public interface TeachingResourceService {

	TeachingResourceResponse create(TeachingResourceCreateRequest request);

	TeachingResourceResponse update(Long id, TeachingResourceUpdateRequest request);

	TeachingResourceResponse getById(Long id);

	PageResult<TeachingResourceResponse> page(TeachingResourceQueryRequest request);

	void delete(Long id);

	ResourceUploadPrepareResponse prepareUpload(ResourceUploadPrepareRequest request);

	void reorder(TeachingResourceReorderRequest request);
}
