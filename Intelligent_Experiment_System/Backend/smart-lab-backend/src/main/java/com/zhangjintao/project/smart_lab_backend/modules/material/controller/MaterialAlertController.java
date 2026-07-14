package com.zhangjintao.project.smart_lab_backend.modules.material.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.HandleAlertDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialAlertVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/material/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MaterialAlertController {

    private final MaterialAlertService alertService;
    private final CurrentUserQueryService currentUserQueryService;

    @GetMapping
    public ApiResult<PageResult<MaterialAlertVO>> pageAlerts(@Valid AlertQueryDTO query) {
        return ApiResult.success(alertService.pageAlerts(query));
    }

    @PostMapping("/{id}/handle")
    public ApiResult<Void> handleAlert(@PathVariable Long id,
                                        @RequestBody(required = false) HandleAlertDTO dto) {
        Long handlerId = currentUserQueryService.requireCurrentUser().getPersonId();
        String result = dto != null ? dto.getResult() : null;
        alertService.handleAlert(id, handlerId, result);
        return ApiResult.success();
    }
}
