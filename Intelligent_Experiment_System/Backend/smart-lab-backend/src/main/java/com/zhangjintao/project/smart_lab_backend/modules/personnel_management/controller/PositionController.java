package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PositionResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personnel/positions")
@RequiredArgsConstructor
public class PositionController {

	private final PositionService positionService;

	@PostMapping
	public ApiResult<PositionResponse> create(@Valid @RequestBody PositionCreateRequest request) {
		return ApiResult.success(positionService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResult<PositionResponse> update(@PathVariable Long id, @Valid @RequestBody PositionUpdateRequest request) {
		return ApiResult.success(positionService.update(id, request));
	}

	@GetMapping("/{id}")
	public ApiResult<PositionResponse> getById(@PathVariable Long id) {
		return ApiResult.success(positionService.getById(id));
	}

	@GetMapping
	public ApiResult<PageResult<PositionResponse>> page(@RequestParam(defaultValue = "1") Long pageNum,
			@RequestParam(defaultValue = "10") Long pageSize,
			@RequestParam(required = false) Long departmentId,
			@RequestParam(required = false) Integer positionStatus) {
		return ApiResult.success(positionService.page(pageNum, pageSize, departmentId, positionStatus));
	}

	@PostMapping("/{id}/status")
	public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer positionStatus) {
		positionService.updateStatus(id, positionStatus);
		return ApiResult.success();
	}
}
