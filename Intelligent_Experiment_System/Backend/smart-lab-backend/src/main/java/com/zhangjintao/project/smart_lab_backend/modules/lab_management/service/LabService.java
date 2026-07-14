package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabStatusUpdateResponse;

public interface LabService {

	LabDetailResponse create(LabCreateCommand command);

	LabDetailResponse update(Long labId, LabUpdateCommand command);

	LabStatusUpdateResponse updateStatus(Long labId, LabStatusUpdateCommand command);

	void delete(Long labId);

	PageResult<LabResponse> page(LabQueryCriteria criteria);

	LabDetailResponse getById(Long labId);
}
