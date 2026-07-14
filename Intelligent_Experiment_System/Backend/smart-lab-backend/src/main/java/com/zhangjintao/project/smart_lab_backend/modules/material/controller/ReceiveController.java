package com.zhangjintao.project.smart_lab_backend.modules.material.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveLogQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.RejectDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveDetailVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveLogVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialReceiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material/receives")
@RequiredArgsConstructor
public class ReceiveController {

    private final MaterialReceiveService receiveService;
    private final CurrentUserQueryService currentUserQueryService;

    @PostMapping("/apply")
    public ApiResult<ReceiveApplyVO> apply(@Valid @RequestBody ReceiveApplyDTO dto) {
        Long applicantId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(receiveService.apply(dto, applicantId));
    }

    @PutMapping("/{applyNo}/cancel")
    public ApiResult<Void> cancel(@PathVariable String applyNo) {
        Long applicantId = currentUserQueryService.requireCurrentUser().getPersonId();
        receiveService.cancel(applyNo, applicantId);
        return ApiResult.success();
    }

    @PostMapping("/{applyNo}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<ReceiveLogVO> approve(@PathVariable String applyNo) {
        Long approverId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(receiveService.approve(applyNo, approverId));
    }

    @PostMapping("/{applyNo}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> reject(@PathVariable String applyNo,
                                   @Valid @RequestBody RejectDTO dto) {
        Long approverId = currentUserQueryService.requireCurrentUser().getPersonId();
        receiveService.reject(applyNo, approverId, dto.getReason());
        return ApiResult.success();
    }

    @GetMapping
    public ApiResult<PageResult<ReceiveApplyVO>> pageApplyRecords(@Valid ReceiveLogQueryDTO query) {
        return ApiResult.success(receiveService.pageApplyRecords(query));
    }

    @GetMapping("/{applyNo}")
    public ApiResult<ReceiveDetailVO> getReceiveDetail(@PathVariable String applyNo) {
        return ApiResult.success(receiveService.getReceiveDetail(applyNo));
    }
}
