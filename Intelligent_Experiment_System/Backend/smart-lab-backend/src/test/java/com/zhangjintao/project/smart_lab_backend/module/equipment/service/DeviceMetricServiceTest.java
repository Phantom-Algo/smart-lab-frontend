package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentMetricIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentMetricIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceMetricServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceMetricServiceTest {

    @Mock private EquipmentMetricIndicatorRepository metricIndicatorRepository;
    @InjectMocks private DeviceMetricServiceImpl metricService;

    @Test
    void testGetMetricsByTypeId() {
        when(metricIndicatorRepository.findByEquipmentTypeId(1L)).thenReturn(List.of());
        var result = metricService.getMetricsByTypeId(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteMetric() {
        EquipmentMetricIndicatorDO indicator = new EquipmentMetricIndicatorDO();
        indicator.setId(1L);
        indicator.setEquipmentTypeId(1L);
        indicator.setMetricName("temp");
        when(metricIndicatorRepository.findById(1L)).thenReturn(Optional.of(indicator));

        assertDoesNotThrow(() -> metricService.deleteMetric(1L));
        verify(metricIndicatorRepository).deleteById(1L);
    }
}
