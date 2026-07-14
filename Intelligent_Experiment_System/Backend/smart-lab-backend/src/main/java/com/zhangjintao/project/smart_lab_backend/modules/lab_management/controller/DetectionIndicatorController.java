package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorService;
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
@RequestMapping("/api/lab-management")
@RequiredArgsConstructor
public class DetectionIndicatorController {

	private final DetectionIndicatorService detectionIndicatorService;

	@PostMapping("/detection-indicators")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<DetectionIndicatorResponse> create(@Valid @RequestBody CreateDetectionIndicatorRequest request) {
		return ApiResult.success(detectionIndicatorService.create(LabManagementConverter.toCreateCommand(request)));
	}

	@PutMapping("/detection-indicators/{indicatorId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<DetectionIndicatorResponse> update(
			@PathVariable @Positive(message = "检测指标ID必须为正数") Long indicatorId,
			@Valid @RequestBody UpdateDetectionIndicatorRequest request) {
		return ApiResult.success(detectionIndicatorService.update(indicatorId,
				LabManagementConverter.toUpdateCommand(request)));
	}

	@DeleteMapping("/detection-indicators/{indicatorId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<Void> delete(@PathVariable @Positive(message = "检测指标ID必须为正数") Long indicatorId) {
		detectionIndicatorService.delete(indicatorId);
		return ApiResult.success();
	}

	@GetMapping("/lab-types/{labTypeId}/indicators")
	public ApiResult<List<DetectionIndicatorResponse>> listByLabTypeId(
			@PathVariable @Positive(message = "实验室类型ID必须为正数") Long labTypeId) {
		return ApiResult.success(detectionIndicatorService.listByLabTypeId(labTypeId));
	}
}
