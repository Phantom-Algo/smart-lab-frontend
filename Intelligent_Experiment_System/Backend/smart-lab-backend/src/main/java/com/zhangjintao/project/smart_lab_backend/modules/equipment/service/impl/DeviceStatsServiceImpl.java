package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairCostDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceBorrowLogMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairCostMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.BorrowStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.DeviceStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.CostStatsVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceHealthVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.FaultStatsVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.UsageStatsVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceHealthCalculator;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatsServiceImpl implements DeviceStatsService {

    private final DeviceMapper deviceMapper;
    private final DeviceBorrowLogMapper borrowLogMapper;
    private final DeviceRepairRecordMapper repairRecordMapper;
    private final DeviceRepairCostMapper repairCostMapper;
    private final DeviceHealthCalculator healthCalculator;

    @Override
    public UsageStatsVO getUsageStats(LocalDateTime start, LocalDateTime end, Long labId) {
        long windowDays = Math.max(1, ChronoUnit.DAYS.between(start, end));
        LocalDateTime now = LocalDateTime.now();

        // 非报废设备
        LambdaQueryWrapper<DeviceDO> deviceQw = new LambdaQueryWrapper<DeviceDO>()
                .ne(DeviceDO::getDeviceStatus, DeviceStatus.SCRAPPED.getCode());
        if (labId != null) deviceQw.eq(DeviceDO::getLabId, labId);
        List<DeviceDO> devices = deviceMapper.selectList(deviceQw);
        long totalDevices = devices.size();

        if (devices.isEmpty()) {
            return UsageStatsVO.builder().totalDevices(0).borrowedDevices(0)
                    .totalBorrowDays(0).usageRate(BigDecimal.ZERO).build();
        }

        List<Long> deviceIds = devices.stream().map(DeviceDO::getId).collect(Collectors.toList());

        // 时间窗口内的借用日志（借出中+已归还），按actualOutTime过滤
        List<DeviceBorrowLogDO> logs = borrowLogMapper.selectList(
                new LambdaQueryWrapper<DeviceBorrowLogDO>()
                        .in(DeviceBorrowLogDO::getDeviceId, deviceIds)
                        .in(DeviceBorrowLogDO::getStatus, BorrowStatus.BORROWING.getCode(), BorrowStatus.RETURNED.getCode())
                        .ge(DeviceBorrowLogDO::getActualOutTime, start)
                        .le(DeviceBorrowLogDO::getActualOutTime, end));

        // 按设备汇总借出小时数
        Map<Long, Long> deviceHours = new HashMap<>();
        for (DeviceBorrowLogDO log : logs) {
            LocalDateTime returnTime = log.getActualReturnTime();
            // 借出中：用当前时间作为临时归还时间（PRD FR-0013）
            if (returnTime == null && log.getStatus() == BorrowStatus.BORROWING.getCode()) {
                returnTime = now;
            }
            if (returnTime != null) {
                long hours = ChronoUnit.HOURS.between(log.getActualOutTime(), returnTime);
                deviceHours.merge(log.getDeviceId(), Math.max(0, hours), Long::sum);
            }
        }

        long totalBorrowDays = deviceHours.values().stream().mapToLong(h -> h / 24).sum();
        long borrowedDevices = deviceHours.size();

        // 使用率 = 累计借出天数 / (设备数 × 窗口天数) × 100%
        BigDecimal rate = BigDecimal.valueOf(totalBorrowDays)
                .divide(BigDecimal.valueOf(totalDevices * windowDays), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return UsageStatsVO.builder().totalDevices(totalDevices).borrowedDevices(borrowedDevices)
                .totalBorrowDays(totalBorrowDays).usageRate(rate).build();
    }

    @Override
    public FaultStatsVO getFaultStats(LocalDateTime start, LocalDateTime end, Long labId) {
        LambdaQueryWrapper<DeviceDO> deviceQw = new LambdaQueryWrapper<DeviceDO>()
                .ne(DeviceDO::getDeviceStatus, DeviceStatus.SCRAPPED.getCode());
        if (labId != null) deviceQw.eq(DeviceDO::getLabId, labId);
        long totalDevices = deviceMapper.selectCount(deviceQw);

        // 按 reportTime 过滤维修记录（PRD FR-0014）
        LambdaQueryWrapper<DeviceRepairRecordDO> repairQw = new LambdaQueryWrapper<DeviceRepairRecordDO>()
                .ge(DeviceRepairRecordDO::getReportTime, start)
                .le(DeviceRepairRecordDO::getReportTime, end);
        if (labId != null) {
            List<Long> deviceIds = deviceMapper.selectList(deviceQw).stream()
                    .map(DeviceDO::getId).collect(Collectors.toList());
            if (deviceIds.isEmpty()) {
                return FaultStatsVO.builder().totalDevices(totalDevices).faultDevices(0)
                        .totalRepairs(0).faultRate(BigDecimal.ZERO).build();
            }
            repairQw.in(DeviceRepairRecordDO::getDeviceId, deviceIds);
        }
        List<DeviceRepairRecordDO> repairs = repairRecordMapper.selectList(repairQw);
        long faultDevices = repairs.stream().map(DeviceRepairRecordDO::getDeviceId).distinct().count();

        BigDecimal rate = totalDevices == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(faultDevices).divide(BigDecimal.valueOf(totalDevices), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return FaultStatsVO.builder().totalDevices(totalDevices).faultDevices(faultDevices)
                .totalRepairs(repairs.size()).faultRate(rate).build();
    }

    @Override
    public CostStatsVO getCostStats(LocalDateTime start, LocalDateTime end, Long labId) {
        // 按时间范围查维修记录
        LambdaQueryWrapper<DeviceRepairRecordDO> repairQw = new LambdaQueryWrapper<DeviceRepairRecordDO>()
                .ge(DeviceRepairRecordDO::getCreatedAt, start)
                .le(DeviceRepairRecordDO::getCreatedAt, end);
        if (labId != null) {
            List<Long> deviceIds = deviceMapper.selectList(
                    new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getLabId, labId))
                    .stream().map(DeviceDO::getId).collect(Collectors.toList());
            if (deviceIds.isEmpty()) {
                return CostStatsVO.builder().totalRepairs(0).totalCost(BigDecimal.ZERO)
                        .avgCost(BigDecimal.ZERO).maxCost(BigDecimal.ZERO).build();
            }
            repairQw.in(DeviceRepairRecordDO::getDeviceId, deviceIds);
        }
        List<DeviceRepairRecordDO> repairs = repairRecordMapper.selectList(repairQw);

        if (repairs.isEmpty()) {
            return CostStatsVO.builder().totalRepairs(0).totalCost(BigDecimal.ZERO)
                    .avgCost(BigDecimal.ZERO).maxCost(BigDecimal.ZERO).build();
        }

        // N+1优化：一次查询所有费用，按repairId分组
        List<Long> repairIds = repairs.stream().map(DeviceRepairRecordDO::getId).collect(Collectors.toList());
        List<DeviceRepairCostDO> allCosts = repairCostMapper.selectList(
                new LambdaQueryWrapper<DeviceRepairCostDO>().in(DeviceRepairCostDO::getRepairId, repairIds));
        Map<Long, List<DeviceRepairCostDO>> costsByRepairId = allCosts.stream()
                .collect(Collectors.groupingBy(DeviceRepairCostDO::getRepairId));

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal maxCost = BigDecimal.ZERO;
        for (DeviceRepairRecordDO repair : repairs) {
            for (DeviceRepairCostDO cost : costsByRepairId.getOrDefault(repair.getId(), Collections.emptyList())) {
                if (cost.getCost() != null) {
                    totalCost = totalCost.add(cost.getCost());
                    if (cost.getCost().compareTo(maxCost) > 0) maxCost = cost.getCost();
                }
            }
        }

        BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(repairs.size()), 2, RoundingMode.HALF_UP);
        return CostStatsVO.builder().totalRepairs(repairs.size()).totalCost(totalCost)
                .avgCost(avgCost).maxCost(maxCost).build();
    }

    @Override
    public List<DeviceHealthVO> getHealthScores(Long labId) {
        LambdaQueryWrapper<DeviceDO> qw = new LambdaQueryWrapper<DeviceDO>()
                .ne(DeviceDO::getDeviceStatus, DeviceStatus.SCRAPPED.getCode());
        if (labId != null) qw.eq(DeviceDO::getLabId, labId);
        List<DeviceDO> devices = deviceMapper.selectList(qw);

        if (devices.isEmpty()) return Collections.emptyList();

        // 批量获取因子（一次DB查询）
        List<Long> deviceIds = devices.stream().map(DeviceDO::getId).collect(Collectors.toList());
        Map<Long, Map<String, Integer>> allFactors = healthCalculator.calculateFactors(deviceIds);

        return devices.stream().map(d -> {
            int score = d.getHealthScore() != null ? d.getHealthScore() : 0;
            String grade = score >= 80 ? "健康" : score >= 60 ? "良好" : score >= 40 ? "关注" : score >= 20 ? "警告" : "危险";
            Map<String, Integer> factors = allFactors.getOrDefault(d.getId(), Collections.emptyMap());
            return DeviceHealthVO.builder()
                    .deviceId(d.getId()).deviceName(d.getName())
                    .healthScore(score).grade(grade)
                    .yearFactor(factors.getOrDefault("yearFactor", 0))
                    .warrantyFactor(factors.getOrDefault("warrantyFactor", 0))
                    .borrowFactor(factors.getOrDefault("borrowFactor", 0))
                    .repairFactor(factors.getOrDefault("repairFactor", 0))
                    .costFactor(factors.getOrDefault("costFactor", 0))
                    .alertFactor(factors.getOrDefault("alertFactor", 0))
                    .scrapAdvisory(score <= 39)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void calculateHealthScores() {
        healthCalculator.recalculateAll();
    }
}
