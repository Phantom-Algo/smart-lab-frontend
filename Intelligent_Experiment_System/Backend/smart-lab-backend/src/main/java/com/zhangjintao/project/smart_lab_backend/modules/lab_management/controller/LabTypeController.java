package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/lab-management/lab-types")
@RequiredArgsConstructor
public class LabTypeController {

	private final LabTypeService labTypeService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabTypeResponse> create(@Valid @RequestBody CreateLabTypeRequest request) {
		return ApiResult.success(labTypeService.create(LabManagementConverter.toCreateCommand(request)));
	}

	@PutMapping("/{typeId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabTypeResponse> update(@PathVariable @Positive(message = "实验室类型ID必须为正数") Long typeId,
			@Valid @RequestBody UpdateLabTypeRequest request) {
		return ApiResult.success(labTypeService.update(typeId, LabManagementConverter.toUpdateCommand(request)));
	}

	@DeleteMapping("/{typeId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<Void> delete(@PathVariable @Positive(message = "实验室类型ID必须为正数") Long typeId) {
		labTypeService.delete(typeId);
		return ApiResult.success();
	}

	@GetMapping
	public ApiResult<List<LabTypeResponse>> listAll() {
		return ApiResult.success(labTypeService.listAll());
	}
}
