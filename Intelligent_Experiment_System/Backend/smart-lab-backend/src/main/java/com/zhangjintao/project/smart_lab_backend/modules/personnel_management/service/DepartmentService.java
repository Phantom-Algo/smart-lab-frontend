package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentTreeNodeResponse;

import java.util.List;

public interface DepartmentService {

	DepartmentResponse create(DepartmentCreateRequest request);

	DepartmentResponse update(Long id, DepartmentUpdateRequest request);

	DepartmentResponse getById(Long id);

	List<DepartmentTreeNodeResponse> getTree();

	void updateStatus(Long id, Integer departmentStatus);
}
