package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceStatsService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/equipment/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DeviceStatsService statsService;

    @GetMapping("/usage")
    public ApiResult<UsageStatsVO> getUsageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Long labId) {
        return ApiResult.success(statsService.getUsageStats(start, end, labId));
    }

    @GetMapping("/fault")
    public ApiResult<FaultStatsVO> getFaultStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Long labId) {
        return ApiResult.success(statsService.getFaultStats(start, end, labId));
    }

    @GetMapping("/cost")
    public ApiResult<CostStatsVO> getCostStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) Long labId) {
        return ApiResult.success(statsService.getCostStats(start, end, labId));
    }

    @GetMapping("/health")
    public ApiResult<List<DeviceHealthVO>> getHealthScores(@RequestParam(required = false) Long labId) {
        return ApiResult.success(statsService.getHealthScores(labId));
    }

    @PostMapping("/health/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> recalculateHealthScores() {
        statsService.calculateHealthScores();
        return ApiResult.success();
    }
}
