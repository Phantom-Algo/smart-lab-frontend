package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.DeviceStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceHealthCalculator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHealthCalculatorImpl implements DeviceHealthCalculator {

    private final DeviceMapper deviceMapper;
    private final DeviceBorrowLogMapper borrowLogMapper;
    private final DeviceRepairRecordMapper repairRecordMapper;
    private final DeviceRepairCostMapper repairCostMapper;
    private final DeviceAlertRecordMapper alertRecordMapper;
    private final MessagePublisher messagePublisher;

    @Override
    public int calculate(Long deviceId) {
        DeviceDO device = deviceMapper.selectById(deviceId);
        if (device == null) return 0;
        return doCalculate(device);
    }

    @Override
    public void recalculateAll() {
        List<DeviceDO> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<DeviceDO>().ne(DeviceDO::getDeviceStatus, DeviceStatus.SCRAPPED.getCode()));
        Set<Long> notifiedThisRun = new HashSet<>();
        for (DeviceDO device : devices) {
            int score = doCalculate(device);
            device.setHealthScore(score);
            deviceMapper.updateById(device);
            // 报废预警幂等：本批次内不重复推送，且已有预警等级的跳过
            if (!notifiedThisRun.contains(device.getId()) && (device.getHealthScore() == null || device.getHealthScore() > 39)) {
                checkScrapAdvisory(device, notifiedThisRun);
            }
        }
        log.info("健康分批量计算完成，设备数={}", devices.size());
    }

    @Override
    public Map<Long, Map<String, Integer>> calculateFactors(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Collections.emptyMap();
        // 批量加载设备
        List<DeviceDO> devices = deviceMapper.selectBatchIds(deviceIds);
        Map<Long, DeviceDO> deviceMap = devices.stream().collect(Collectors.toMap(DeviceDO::getId, d -> d, (a, b) -> a));

        // 批量查询借用次数
        Map<Long, Long> borrowCounts = new HashMap<>();
        List<DeviceBorrowLogDO> allBorrowLogs = borrowLogMapper.selectList(
                new LambdaQueryWrapper<DeviceBorrowLogDO>().in(DeviceBorrowLogDO::getDeviceId, deviceIds));
        for (DeviceBorrowLogDO log : allBorrowLogs) {
            borrowCounts.merge(log.getDeviceId(), 1L, Long::sum);
        }

        // 批量查询维修记录数 + 费用
        Map<Long, Long> repairCounts = new HashMap<>();
        Map<Long, BigDecimal> totalCosts = new HashMap<>();
        List<DeviceRepairRecordDO> allRepairs = repairRecordMapper.selectList(
                new LambdaQueryWrapper<DeviceRepairRecordDO>().in(DeviceRepairRecordDO::getDeviceId, deviceIds));
        if (!allRepairs.isEmpty()) {
            List<Long> repairIds = allRepairs.stream().map(DeviceRepairRecordDO::getId).collect(Collectors.toList());
            List<DeviceRepairCostDO> allCosts = repairCostMapper.selectList(
                    new LambdaQueryWrapper<DeviceRepairCostDO>().in(DeviceRepairCostDO::getRepairId, repairIds));
            Map<Long, BigDecimal> costByRepairId = new HashMap<>();
            for (DeviceRepairCostDO c : allCosts) {
                if (c.getCost() != null) {
                    costByRepairId.merge(c.getRepairId(), c.getCost(), BigDecimal::add);
                }
            }
            for (DeviceRepairRecordDO r : allRepairs) {
                repairCounts.merge(r.getDeviceId(), 1L, Long::sum);
                BigDecimal cost = costByRepairId.get(r.getId());
                if (cost != null) totalCosts.merge(r.getDeviceId(), cost, BigDecimal::add);
            }
        }

        // 批量查询告警次数
        Map<Long, Long> alertCounts = new HashMap<>();
        List<DeviceAlertRecordDO> allAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<DeviceAlertRecordDO>().in(DeviceAlertRecordDO::getDeviceId, deviceIds));
        for (DeviceAlertRecordDO a : allAlerts) {
            alertCounts.merge(a.getDeviceId(), 1L, Long::sum);
        }

        // 逐设备计算因子
        Map<Long, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Long deviceId : deviceIds) {
            DeviceDO device = deviceMap.get(deviceId);
            if (device == null) continue;
            long borrowCount = borrowCounts.getOrDefault(deviceId, 0L);
            long repairCount = repairCounts.getOrDefault(deviceId, 0L);
            BigDecimal totalCost = totalCosts.getOrDefault(deviceId, BigDecimal.ZERO);
            long alertCount = alertCounts.getOrDefault(deviceId, 0L);
            result.put(deviceId, computeFactors(device, borrowCount, repairCount, totalCost, alertCount));
        }
        return result;
    }

    // ==================== 私有方法 ====================

    private int doCalculate(DeviceDO device) {
        try {
            long borrowCount = borrowLogMapper.selectCount(
                    new LambdaQueryWrapper<DeviceBorrowLogDO>().eq(DeviceBorrowLogDO::getDeviceId, device.getId()));
            long repairCount = repairRecordMapper.selectCount(
                    new LambdaQueryWrapper<DeviceRepairRecordDO>().eq(DeviceRepairRecordDO::getDeviceId, device.getId()));
            BigDecimal totalCost = calcTotalRepairCost(device.getId());
            long alertCount = alertRecordMapper.selectCount(
                    new LambdaQueryWrapper<DeviceAlertRecordDO>().eq(DeviceAlertRecordDO::getDeviceId, device.getId()));

            Map<String, Integer> factors = computeFactors(device, borrowCount, repairCount, totalCost, alertCount);
            int totalScore = factors.values().stream().mapToInt(Integer::intValue).sum();
            return Math.max(0, Math.min(100, totalScore));
        } catch (Exception e) {
            log.error("计算设备健康分异常，deviceId={}", device.getId(), e);
            return 50;
        }
    }

    private Map<String, Integer> computeFactors(DeviceDO device, long borrowCount,
            long repairCount, BigDecimal totalRepairCost, long alertCount) {
        LocalDate now = LocalDate.now();

        // 因子1: 购置年限 20%
        int yearScore = 100;
        if (device.getPurchaseDate() != null) {
            long years = ChronoUnit.YEARS.between(device.getPurchaseDate(), now);
            yearScore = (int) Math.max(0, 100 - years * 10);
        }
        int yearFactor = (int) (yearScore * 0.20);

        // 因子2: 保修状态 10%
        int warrantyScore = 100;
        if (device.getExpireDate() != null && device.getExpireDate().isBefore(now)) {
            long yearsPast = ChronoUnit.YEARS.between(device.getExpireDate(), now);
            warrantyScore = (int) Math.max(0, 100 - yearsPast * 20);
        }
        int warrantyFactor = (int) (warrantyScore * 0.10);

        // 因子3: 借用频次 15%（按TDD：月均1-4次满分）
        int borrowScore = 100;
        if (device.getPurchaseDate() != null) {
            long monthsActive = Math.max(1, ChronoUnit.MONTHS.between(device.getPurchaseDate(), now));
            double monthlyAvg = (double) borrowCount / monthsActive;
            borrowScore = monthlyAvg <= 0 ? 100 : monthlyAvg <= 4 ? 100 : monthlyAvg <= 8 ? 70 : 40;
        } else if (borrowCount > 20) {
            borrowScore = borrowCount <= 50 ? 60 : 40;
        }
        int borrowFactor = (int) (borrowScore * 0.15);

        // 因子4: 维修次数 25%
        int repairScore = repairCount == 0 ? 100 : repairCount <= 2 ? 80 : repairCount <= 5 ? 50 : 20;
        int repairFactor = (int) (repairScore * 0.25);

        // 因子5: 维修费用占比 20%
        int costScore = 100;
        if (device.getPurchasePrice() != null && device.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0
                && totalRepairCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = totalRepairCost.divide(device.getPurchasePrice(), MathContext.DECIMAL32);
            if (ratio.compareTo(new BigDecimal("0.5")) > 0) costScore = 20;
            else if (ratio.compareTo(new BigDecimal("0.3")) > 0) costScore = 50;
            else if (ratio.compareTo(new BigDecimal("0.1")) > 0) costScore = 80;
        }
        int costFactor = (int) (costScore * 0.20);

        // 因子6: 传感器异常 10%
        int alertScore = alertCount == 0 ? 100 : alertCount <= 3 ? 80 : alertCount <= 10 ? 50 : 20;
        int alertFactor = (int) (alertScore * 0.10);

        Map<String, Integer> factors = new LinkedHashMap<>();
        factors.put("yearFactor", yearFactor);
        factors.put("warrantyFactor", warrantyFactor);
        factors.put("borrowFactor", borrowFactor);
        factors.put("repairFactor", repairFactor);
        factors.put("costFactor", costFactor);
        factors.put("alertFactor", alertFactor);
        return factors;
    }

    private BigDecimal calcTotalRepairCost(Long deviceId) {
        List<DeviceRepairRecordDO> repairs = repairRecordMapper.selectList(
                new LambdaQueryWrapper<DeviceRepairRecordDO>().eq(DeviceRepairRecordDO::getDeviceId, deviceId));
        if (repairs.isEmpty()) return BigDecimal.ZERO;
        List<Long> repairIds = repairs.stream().map(DeviceRepairRecordDO::getId).collect(Collectors.toList());
        List<DeviceRepairCostDO> costs = repairCostMapper.selectList(
                new LambdaQueryWrapper<DeviceRepairCostDO>().in(DeviceRepairCostDO::getRepairId, repairIds));
        BigDecimal total = BigDecimal.ZERO;
        for (DeviceRepairCostDO c : costs) {
            if (c.getCost() != null) total = total.add(c.getCost());
        }
        return total;
    }

    private void checkScrapAdvisory(DeviceDO device, Set<Long> notifiedThisRun) {
        if (device.getPurchasePrice() == null || device.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) return;
        try {
            BigDecimal totalCost = calcTotalRepairCost(device.getId());
            if (totalCost.compareTo(device.getPurchasePrice().multiply(new BigDecimal("0.8"))) >= 0) {
                notifiedThisRun.add(device.getId());
                MessagePublishCommand cmd = MessagePublishCommand.builder()
                        .eventNo(MessageEventNoGenerator.generate("equipment.warning.scrap.advisory"))
                        .sourceSystem("equipment").messageType("equipment.warning.scrap.advisory")
                        .targetType(TargetType.ALL_MANAGER).templateCode("equipment.warning.scrap.advisory")
                        .params(Map.of("deviceName", device.getName(), "totalCost", totalCost.toPlainString(),
                                "purchasePrice", device.getPurchasePrice().toPlainString()))
                        .occurredAt(LocalDateTime.now()).build();
                messagePublisher.publish(cmd);
            }
        } catch (Exception e) {
            log.error("报废预警推送失败，deviceId={}", device.getId(), e);
        }
    }
}
