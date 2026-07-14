package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceAlertRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceAlertServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceAlertServiceTest {

    @Mock private DeviceAlertRecordRepository alertRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private PersonDirectoryPort personDirectoryPort;
    @InjectMocks private DeviceAlertServiceImpl alertService;

    @Test
    void testHandleAlert_ShouldUpdateStatus() {
        DeviceAlertRecordDO alert = new DeviceAlertRecordDO();
        alert.setId(1L);
        alert.setStatus(0); // UNHANDLED
        alert.setDeviceId(100L);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(personDirectoryPort.getByPersonId(200L)).thenReturn(Optional.of(person()));

        assertDoesNotThrow(() -> alertService.handleAlert(1L, 200L, "normal"));
        verify(alertRepository).updateHandle(1L, 200L, "normal");
    }

    @Test
    void testHandleAlert_AlreadyHandled_ShouldThrow() {
        DeviceAlertRecordDO alert = new DeviceAlertRecordDO();
        alert.setId(1L);
        alert.setStatus(1); // HANDLED

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(personDirectoryPort.getByPersonId(200L)).thenReturn(Optional.of(person()));
        assertThrows(EquipmentException.class,
                () -> alertService.handleAlert(1L, 200L, "normal"));
    }

    private PersonDirectoryItemDTO person() {
        return PersonDirectoryItemDTO.builder().personId(200L).personName("测试管理员").build();
    }
}
