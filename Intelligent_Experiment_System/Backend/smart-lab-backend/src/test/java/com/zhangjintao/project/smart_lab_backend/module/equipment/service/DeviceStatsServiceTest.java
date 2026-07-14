package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceHealthCalculator;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceStatsServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceStatsServiceTest {

    @Mock private DeviceMapper deviceMapper;
    @Mock private DeviceBorrowLogMapper borrowLogMapper;
    @Mock private DeviceRepairRecordMapper repairRecordMapper;
    @Mock private DeviceRepairCostMapper repairCostMapper;
    @Mock private DeviceHealthCalculator healthCalculator;
    @InjectMocks private DeviceStatsServiceImpl statsService;

    @Test
    void testGetUsageStats_NoDevices_ShouldReturnZero() {
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        var result = statsService.getUsageStats(java.time.LocalDateTime.now().minusDays(7),
                java.time.LocalDateTime.now(), null);

        assertEquals(0, result.getTotalDevices());
        assertEquals(java.math.BigDecimal.ZERO, result.getUsageRate());
    }

    @Test
    void testCalculateHealthScores_ShouldDelegateToCalculator() {
        statsService.calculateHealthScores();
        verify(healthCalculator).recalculateAll();
    }
}
