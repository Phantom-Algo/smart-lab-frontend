package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairCostMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairCostRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceRepairServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceRepairServiceTest {

    @Mock private DeviceRepairRecordRepository repairRepository;
    @Mock private DeviceRepairCostRepository costRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceRepairRecordMapper repairRecordMapper;
    @Mock private DeviceRepairCostMapper repairCostMapper;
    @Mock private EquipmentConfig equipmentConfig;
    @Mock private MessagePublisher messagePublisher;
    @Mock private PersonDirectoryPort personDirectoryPort;
    @InjectMocks private DeviceRepairServiceImpl repairService;

    @Test
    void testAcceptRepair_ValidStatus_ShouldUpdate() {
        DeviceRepairRecordDO record = new DeviceRepairRecordDO();
        record.setId(1L);
        record.setRepairNo("R001");
        record.setDeviceId(1L);
        record.setRepairStatus(3); // REPAIRED
        when(repairRepository.findByRepairNo("R001")).thenReturn(Optional.of(record));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO()));

        assertDoesNotThrow(() -> repairService.acceptRepair("R001"));
        verify(repairRepository).updateById(record);
    }

    @Test
    void testRecordCost_ShouldSave() {
        DeviceRepairRecordDO record = new DeviceRepairRecordDO();
        record.setId(1L);
        record.setRepairNo("R001");
        record.setRepairStatus(4); // ACCEPTED
        when(repairRepository.findByRepairNo("R001")).thenReturn(Optional.of(record));

        assertDoesNotThrow(() -> repairService.recordCost("R001", BigDecimal.valueOf(500), "零件费"));
        verify(costRepository).insert(any());
    }
}
