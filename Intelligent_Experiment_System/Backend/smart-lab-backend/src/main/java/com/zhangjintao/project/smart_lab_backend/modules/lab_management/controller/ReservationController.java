package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.ApproveReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CancelReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.MyReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.PendingReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.ScheduleOccupancyQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/lab-management")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	private final CurrentUserQueryService currentUserQueryService;

	@PostMapping("/reservations")
	@PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
	public ApiResult<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.create(LabManagementConverter.toCreateCommand(request, currentUserId)));
	}

	@PatchMapping("/reservations/{reservationNo}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<ReservationResponse> approve(
			@PathVariable @NotBlank(message = "预约单号不能为空") @Size(max = 32, message = "预约单号长度不能超过32")
			String reservationNo,
			@Valid @RequestBody ApproveReservationRequest request) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.approve(
				LabManagementConverter.toApproveCommand(reservationNo, request, currentUserId)));
	}

	@PatchMapping("/reservations/{reservationNo}/cancel")
	public ApiResult<ReservationResponse> cancel(
			@PathVariable @NotBlank(message = "预约单号不能为空") @Size(max = 32, message = "预约单号长度不能超过32")
			String reservationNo,
			@Valid @RequestBody CancelReservationRequest request) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.cancel(
				LabManagementConverter.toCancelCommand(reservationNo, request, currentUserId)));
	}

	@PatchMapping("/reservations/{reservationNo}/check-in")
	public ApiResult<ReservationResponse> checkIn(
			@PathVariable @NotBlank(message = "预约单号不能为空") @Size(max = 32, message = "预约单号长度不能超过32")
			String reservationNo) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.checkIn(reservationNo, currentUserId));
	}

	@PatchMapping("/reservations/{reservationNo}/admin-check-in")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<ReservationResponse> adminCheckIn(
			@PathVariable @NotBlank(message = "预约单号不能为空") @Size(max = 32, message = "预约单号长度不能超过32")
			String reservationNo) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.adminCheckIn(reservationNo, currentUserId));
	}

	@PatchMapping("/reservations/{reservationNo}/check-out")
	public ApiResult<ReservationResponse> checkOut(
			@PathVariable @NotBlank(message = "预约单号不能为空") @Size(max = 32, message = "预约单号长度不能超过32")
			String reservationNo) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.checkOut(reservationNo, currentUserId));
	}

	@GetMapping("/reservations/my")
	public ApiResult<PageResult<ReservationResponse>> pageMine(@Valid @ModelAttribute MyReservationPageQueryRequest request) {
		Long currentUserId = currentUserQueryService.requireCurrentUser().getUserId();
		return ApiResult.success(reservationService.pageMine(LabManagementConverter.toCriteria(request, currentUserId)));
	}

	@GetMapping("/reservations")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<PageResult<ReservationResponse>> pageAll(@Valid @ModelAttribute MyReservationPageQueryRequest request) {
		return ApiResult.success(reservationService.pageAll(LabManagementConverter.toCriteria(request)));
	}

	@GetMapping("/reservations/pending")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<PageResult<ReservationResponse>> pagePending(
			@Valid @ModelAttribute PendingReservationPageQueryRequest request) {
		return ApiResult.success(reservationService.pagePending(LabManagementConverter.toCriteria(request)));
	}

	@GetMapping("/labs/{labId}/schedule-occupancy")
	public ApiResult<LabOccupancyResponse> getOccupancy(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId,
			@Valid @ModelAttribute ScheduleOccupancyQueryRequest request) {
		return ApiResult.success(reservationService.getOccupancy(labId, request.getDate()));
	}
}
