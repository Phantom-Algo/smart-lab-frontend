package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.LabPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabStatusRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabStatusUpdateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/lab-management/labs")
@RequiredArgsConstructor
public class LabController {

	private final LabService labService;
	private final CurrentUserQueryService currentUserQueryService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabDetailResponse> create(@Valid @RequestBody CreateLabRequest request) {
		return ApiResult.success(labService.create(LabManagementConverter.toCreateCommand(request)));
	}

	@PutMapping("/{labId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabDetailResponse> update(@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @RequestBody UpdateLabRequest request) {
		return ApiResult.success(labService.update(labId, LabManagementConverter.toUpdateCommand(request)));
	}

	@PatchMapping("/{labId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabStatusUpdateResponse> updateStatus(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @RequestBody UpdateLabStatusRequest request) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(labService.updateStatus(labId,
				LabManagementConverter.toStatusUpdateCommand(request, currentUserId)));
	}

	@DeleteMapping("/{labId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<Void> delete(@PathVariable @Positive(message = "实验室ID必须为正数") Long labId) {
		labService.delete(labId);
		return ApiResult.success();
	}

	@GetMapping
	public ApiResult<PageResult<LabResponse>> page(@Valid @ModelAttribute LabPageQueryRequest request) {
		return ApiResult.success(labService.page(LabManagementConverter.toCriteria(request)));
	}

	@GetMapping("/{labId}")
	public ApiResult<LabDetailResponse> getById(@PathVariable @Positive(message = "实验室ID必须为正数") Long labId) {
		return ApiResult.success(labService.getById(labId));
	}
}
