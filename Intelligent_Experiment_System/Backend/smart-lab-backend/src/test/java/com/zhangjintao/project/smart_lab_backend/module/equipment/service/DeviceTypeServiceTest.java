package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceTypeCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceTypeServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class DeviceTypeServiceTest {

    @Mock private EquipmentTypeRepository equipmentTypeRepository;
    @Mock private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceTypeServiceImpl typeService;

    @Test
    void testListTypes_ShouldReturnAll() {
        EquipmentTypeDO t1 = new EquipmentTypeDO(); t1.setId(1L); t1.setTypeName("精密仪器");
        EquipmentTypeDO t2 = new EquipmentTypeDO(); t2.setId(2L); t2.setTypeName("通用设备");
        when(equipmentTypeRepository.findAll()).thenReturn(List.of(t1, t2));

        var result = typeService.listTypes();
        assertEquals(2, result.size());
    }

    @Test
    void testCreateType_ShouldCallInsert() {
        DeviceTypeCreateDTO dto = new DeviceTypeCreateDTO();
        dto.setTypeName("分析仪器");
        when(equipmentTypeRepository.findByTypeName("分析仪器")).thenReturn(Optional.empty());

        var result = typeService.createType(dto);
        verify(equipmentTypeRepository).insert(any());
        assertEquals("分析仪器", result.getTypeName());
    }

    @Test
    void testDeleteType_NoDeviceInUse_ShouldSucceed() {
        when(deviceRepository.findByDeviceTypeId(1L)).thenReturn(List.of());
        when(equipmentTypeRepository.findById(1L))
                .thenReturn(Optional.of(new EquipmentTypeDO() {{
                    setId(1L); setTypeName("测试");
                }}));

        assertDoesNotThrow(() -> typeService.deleteType(1L));
        verify(equipmentTypeRepository).deleteById(1L);
    }
}
