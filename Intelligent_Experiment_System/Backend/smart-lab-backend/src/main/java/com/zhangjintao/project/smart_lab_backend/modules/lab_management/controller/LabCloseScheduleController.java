package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.LabCloseScheduleQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabCloseScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/lab-management/labs/{labId}/close-schedules")
@RequiredArgsConstructor
public class LabCloseScheduleController {

	private final LabCloseScheduleService labCloseScheduleService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabCloseScheduleMutationResponse> create(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @RequestBody CreateLabCloseScheduleRequest request) {
		return ApiResult.success(labCloseScheduleService.create(labId,
				LabManagementConverter.toCreateCommand(request)));
	}

	@PutMapping("/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabCloseScheduleMutationResponse> update(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@PathVariable @Positive(message = "临时关闭时间段ID必须为正数") Long scheduleId,
			@Valid @RequestBody UpdateLabCloseScheduleRequest request) {
		return ApiResult.success(labCloseScheduleService.update(labId, scheduleId,
				LabManagementConverter.toUpdateCommand(request)));
	}

	@DeleteMapping("/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<Void> delete(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@PathVariable @Positive(message = "临时关闭时间段ID必须为正数") Long scheduleId) {
		labCloseScheduleService.delete(labId, scheduleId);
		return ApiResult.success();
	}

	@GetMapping
	public ApiResult<List<LabCloseScheduleResponse>> listByLabId(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @ModelAttribute LabCloseScheduleQueryRequest request) {
		return ApiResult.success(labCloseScheduleService.listByLabId(labId,
				LabManagementConverter.toCriteria(request)));
	}
}
