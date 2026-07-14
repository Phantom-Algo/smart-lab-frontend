package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.MetricIndicatorConfigDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.MetricIndicatorVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceMetricService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/equipment/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final DeviceMetricService metricService;

    @GetMapping
    public ApiResult<List<MetricIndicatorVO>> getMetricsByTypeId(@RequestParam Long typeId) {
        return ApiResult.success(metricService.getMetricsByTypeId(typeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<MetricIndicatorVO> configureMetric(@Valid @RequestBody MetricIndicatorConfigDTO dto) {
        return ApiResult.success(metricService.configureMetric(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> deleteMetric(@PathVariable Long id) {
        metricService.deleteMetric(id);
        return ApiResult.success();
    }
}
