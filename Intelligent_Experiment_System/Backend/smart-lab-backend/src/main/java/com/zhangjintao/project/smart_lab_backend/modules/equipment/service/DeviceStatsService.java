package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.CostStatsVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceHealthVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.FaultStatsVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.UsageStatsVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备统计分析服务接口
 */
public interface DeviceStatsService {
    UsageStatsVO getUsageStats(LocalDateTime start, LocalDateTime end, Long labId);
    FaultStatsVO getFaultStats(LocalDateTime start, LocalDateTime end, Long labId);
    CostStatsVO getCostStats(LocalDateTime start, LocalDateTime end, Long labId);
    List<DeviceHealthVO> getHealthScores(Long labId);
    void calculateHealthScores();
}
