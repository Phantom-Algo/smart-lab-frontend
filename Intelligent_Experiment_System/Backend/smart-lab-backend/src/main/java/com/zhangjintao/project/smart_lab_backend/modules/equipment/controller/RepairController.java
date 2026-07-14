package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairReportDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.RepairVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceRepairService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 设备维修管理 Controller
 * <p>
 * 所有涉及人员ID的操作均从 SecurityContext 获取当前登录人的 personId，
 * 不再信任前端传入的 reporterId/handlerId。
 * </p>
 */
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class RepairController {

    private final DeviceRepairService repairService;
    private final CurrentUserQueryService currentUserQueryService;

    @GetMapping("/repairs")
    public ApiResult<PageResult<RepairVO>> listRepairRecords(@Valid RepairQueryDTO query) {
        return ApiResult.success(repairService.listRepairRecords(query));
    }

    @PostMapping("/repairs")
    public ApiResult<RepairVO> reportFault(@Valid @RequestBody RepairReportDTO dto) {
        Long reporterId = currentUserQueryService.requireCurrentUser().getPersonId();
        dto.setReporterId(reporterId);
        return ApiResult.success(repairService.reportFault(dto));
    }

    @PostMapping("/repairs/{repairNo}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> startRepair(@PathVariable String repairNo,
                                        @RequestBody Map<String, String> body) {
        Long handlerId = currentUserQueryService.requireCurrentUser().getPersonId();
        repairService.startRepair(repairNo, handlerId);
        return ApiResult.success();
    }

    @PostMapping("/repairs/{repairNo}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<RepairVO> completeRepair(@PathVariable String repairNo,
                                               @RequestBody Map<String, String> body) {
        return ApiResult.success(repairService.completeRepair(repairNo, body.get("repairDesc")));
    }

    @PostMapping("/repairs/{repairNo}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> acceptRepair(@PathVariable String repairNo) {
        repairService.acceptRepair(repairNo);
        return ApiResult.success();
    }

    @PostMapping("/repairs/{repairNo}/cost")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> recordCost(@PathVariable String repairNo,
                                       @RequestBody Map<String, Object> body) {
        BigDecimal cost = new BigDecimal(body.get("cost").toString());
        String remark = (String) body.get("remark");
        repairService.recordCost(repairNo, cost, remark);
        return ApiResult.success();
    }
}
