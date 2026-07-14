package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PositionResponse;

public interface PositionService {

	PositionResponse create(PositionCreateRequest request);

	PositionResponse update(Long id, PositionUpdateRequest request);

	PositionResponse getById(Long id);

	PageResult<PositionResponse> page(Long pageNum, Long pageSize, Long departmentId, Integer positionStatus);

	void updateStatus(Long id, Integer positionStatus);
}
