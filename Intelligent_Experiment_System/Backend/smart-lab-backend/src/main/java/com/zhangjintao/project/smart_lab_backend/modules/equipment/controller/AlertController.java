package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.AlertRecordVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceAlertService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 设备告警管理 Controller
 * <p>
 * handlerId 从 SecurityContext 获取当前登录人的 personId，不再信任前端传入。
 * </p>
 */
@RestController
@RequestMapping("/api/equipment/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final DeviceAlertService alertService;
    private final CurrentUserQueryService currentUserQueryService;

    @GetMapping
    public ApiResult<PageResult<AlertRecordVO>> listAlerts(@Valid AlertQueryDTO query) {
        return ApiResult.success(alertService.listAlerts(query));
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> handleAlert(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        Long handlerId = currentUserQueryService.requireCurrentUser().getPersonId();
        String result = (String) body.get("result");
        alertService.handleAlert(id, handlerId, result);
        return ApiResult.success();
    }
}
