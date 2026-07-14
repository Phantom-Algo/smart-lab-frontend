package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorBindingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private EquipmentTypeRepository equipmentTypeRepository;
    @Mock private DeviceMapper deviceMapper;
    @Mock private EquipmentConfig equipmentConfig;
    @Mock private DeviceSensorBindingService deviceSensorBindingService;
    @InjectMocks private DeviceServiceImpl deviceService;

    @Test
    void testGetDevice_ShouldReturnDevice() {
        DeviceDO device = new DeviceDO();
        device.setId(1L);
        device.setDeviceNo("EQ001");
        device.setName("离心机");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        var result = deviceService.getDevice(1L);
        assertNotNull(result);
        assertEquals("EQ001", result.getDeviceNo());
    }

    @Test
    void testGetDevice_NotFound_ShouldThrow() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> deviceService.getDevice(999L));
    }

    @Test
    void testDeleteDevice_ShouldCallRepository() {
        DeviceDO device = new DeviceDO();
        device.setId(1L);
        device.setDeviceStatus(5);
        when(deviceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(device));

        assertDoesNotThrow(() -> deviceService.deleteDevice(1L));
        verify(deviceRepository).deleteById(1L);
    }

    @Test
    void testDeleteDevice_WithActiveSensorBinding_ShouldReject() {
        DeviceDO device = new DeviceDO();
        device.setId(1L);
        device.setDeviceStatus(5);
        when(deviceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(device));
        when(deviceSensorBindingService.existsActiveBindingByDeviceId(1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> deviceService.deleteDevice(1L));
        verify(deviceRepository, never()).deleteById(1L);
    }
}
