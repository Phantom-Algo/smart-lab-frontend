package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceBorrowApplyMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowApplyRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceBorrowServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceBorrowServiceTest {

    @Mock private DeviceBorrowApplyRepository applyRepository;
    @Mock private DeviceBorrowLogRepository borrowLogRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceBorrowApplyMapper applyMapper;
    @Mock private EquipmentConfig config;
    @Mock private MessagePublisher messagePublisher;
    @Mock private PersonDirectoryPort personDirectoryPort;
    @InjectMocks private DeviceBorrowServiceImpl borrowService;

    @Test
    void testDetectOverdue_NoExpired_DoesNothing() {
        when(borrowLogRepository.findOverdueLogs()).thenReturn(List.of());
        assertDoesNotThrow(() -> borrowService.detectOverdue());
        verify(messagePublisher, never()).publish(any());
    }
}
