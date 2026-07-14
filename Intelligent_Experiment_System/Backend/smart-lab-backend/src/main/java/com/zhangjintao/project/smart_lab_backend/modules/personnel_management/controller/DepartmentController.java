package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentTreeNodeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personnel/departments")
@RequiredArgsConstructor
public class DepartmentController {

	private final DepartmentService departmentService;

	@PostMapping
	public ApiResult<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest request) {
		return ApiResult.success(departmentService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResult<DepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest request) {
		return ApiResult.success(departmentService.update(id, request));
	}

	@GetMapping("/{id}")
	public ApiResult<DepartmentResponse> getById(@PathVariable Long id) {
		return ApiResult.success(departmentService.getById(id));
	}

	@GetMapping("/tree")
	public ApiResult<List<DepartmentTreeNodeResponse>> getTree() {
		return ApiResult.success(departmentService.getTree());
	}

	@PostMapping("/{id}/status")
	public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer departmentStatus) {
		departmentService.updateStatus(id, departmentStatus);
		return ApiResult.success();
	}
}
