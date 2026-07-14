package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import java.util.List;
import java.util.Map;

/**
 * 设备健康分计算引擎
 */
public interface DeviceHealthCalculator {
    int calculate(Long deviceId);
    void recalculateAll();

    /**
     * 批量计算设备健康分因子（供统计分析模块调用，避免N+1查询）
     * @return Map<deviceId, Map<factorName, factorScore>>
     *         factorName: yearFactor/warrantyFactor/borrowFactor/repairFactor/costFactor/alertFactor
     */
    Map<Long, Map<String, Integer>> calculateFactors(List<Long> deviceIds);
}
