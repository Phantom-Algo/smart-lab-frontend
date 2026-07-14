package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonStatusChangeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PersonDetailResponse;

public interface PersonService {

	PersonDetailResponse create(PersonCreateRequest request);

	PersonDetailResponse update(Long id, PersonUpdateRequest request);

	PersonDetailResponse getById(Long id);

	PageResult<PersonDetailResponse> page(PersonQueryRequest request);

	void updateStatus(Long id, PersonStatusChangeRequest request);
}
