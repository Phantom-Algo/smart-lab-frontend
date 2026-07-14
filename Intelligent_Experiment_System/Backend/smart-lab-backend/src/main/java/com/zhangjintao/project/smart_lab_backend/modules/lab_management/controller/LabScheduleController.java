package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.SetLabSchedulesRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/lab-management/labs/{labId}/schedules")
@RequiredArgsConstructor
public class LabScheduleController {

	private final LabScheduleService labScheduleService;

	@PutMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabScheduleMutationResponse> save(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @RequestBody SetLabSchedulesRequest request) {
		return ApiResult.success(labScheduleService.save(labId, LabManagementConverter.toSaveCommand(request)));
	}

	@GetMapping
	public ApiResult<List<LabScheduleResponse>> listByLabId(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId) {
		return ApiResult.success(labScheduleService.listByLabId(labId));
	}

	@DeleteMapping("/{scheduleId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<LabScheduleMutationResponse> delete(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@PathVariable @Positive(message = "开放时间ID必须为正数") Long scheduleId,
			@RequestParam(defaultValue = "false") boolean forceCancelConflictedReservations) {
		return ApiResult.success(labScheduleService.delete(labId, scheduleId, forceCancelConflictedReservations));
	}
}
