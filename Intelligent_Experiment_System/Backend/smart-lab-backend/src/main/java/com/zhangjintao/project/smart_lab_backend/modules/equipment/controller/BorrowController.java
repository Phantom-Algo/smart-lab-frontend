package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowLogVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceBorrowService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 设备借用管理 Controller
 * <p>
 * 所有涉及人员ID的操作均从 SecurityContext 获取当前登录人的 personId，
 * 不再信任前端传入。
 * </p>
 */
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class BorrowController {

    private final DeviceBorrowService borrowService;
    private final CurrentUserQueryService currentUserQueryService;

    @PostMapping("/devices/{id}/borrow")
    public ApiResult<BorrowApplyVO> applyBorrow(@PathVariable("id") Long deviceId,
                                                 @Valid @RequestBody BorrowApplyDTO dto) {
        Long applicantId = currentUserQueryService.requireCurrentUser().getPersonId();
        dto.setApplicantId(applicantId);
        return ApiResult.success(borrowService.apply(deviceId, dto));
    }

    @PostMapping("/borrows/{applyNo}/cancel")
    public ApiResult<Void> cancelApply(@PathVariable String applyNo) {
        borrowService.cancelApply(applyNo);
        return ApiResult.success();
    }

    @PostMapping("/borrows/{applyNo}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> approveBorrow(@PathVariable String applyNo,
                                          @Valid @RequestBody BorrowApproveDTO dto) {
        Long approverId = currentUserQueryService.requireCurrentUser().getPersonId();
        dto.setApproverId(approverId);
        borrowService.approve(applyNo, dto);
        return ApiResult.success();
    }

    @PostMapping("/borrows/{applyNo}/check-out")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<BorrowLogVO> checkOut(@PathVariable String applyNo) {
        Long outOperatorId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(borrowService.checkOut(applyNo, outOperatorId));
    }

    @PostMapping("/borrows/{logId}/check-in")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> checkIn(@PathVariable Long logId,
                                    @RequestBody(required = false) Map<String, String> body) {
        String returnRemark = body != null ? body.get("returnRemark") : null;
        Long returnOperatorId = currentUserQueryService.requireCurrentUser().getPersonId();
        borrowService.checkIn(logId, returnRemark, returnOperatorId);
        return ApiResult.success();
    }

    @GetMapping("/borrows")
    public ApiResult<PageResult<BorrowLogVO>> listBorrowLogs(@Valid BorrowLogQueryDTO query) {
        return ApiResult.success(borrowService.listBorrowLogs(query));
    }

    @GetMapping("/borrow-applies/my")
    public ApiResult<PageResult<BorrowApplyVO>> listMyBorrowApplies(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "100") long pageSize,
            @RequestParam(required = false) Integer applyStatus) {
        Long applicantId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(borrowService.listMyApplies(applicantId, applyStatus, pageNum, pageSize));
    }

    @GetMapping("/borrow-applies")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<PageResult<BorrowApplyVO>> listBorrowApplies(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "100") long pageSize,
            @RequestParam(required = false) String applyNo,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Integer applyStatus) {
        return ApiResult.success(borrowService.listApplies(applyNo, deviceId, applicantId,
                applyStatus, pageNum, pageSize));
    }

    @PostMapping("/borrows/detect-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> detectOverdue() {
        borrowService.detectOverdue();
        return ApiResult.success();
    }
}
