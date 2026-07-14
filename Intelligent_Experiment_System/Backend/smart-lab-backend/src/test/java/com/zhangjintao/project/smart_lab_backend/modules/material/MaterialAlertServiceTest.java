package com.zhangjintao.project.smart_lab_backend.modules.material;

import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialAlertRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialAlertRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialBatchRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialAlertVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.impl.MaterialAlertServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 预警管理服务单元测试 — 7.1 MaterialAlertServiceTest
 * <p>
 * 覆盖：低库存检测触发/不触发/去重、临期检测、过期自动报废、超量领用预警、
 * 预警处理、分页查询
 *
 * @author linbowei
 */
@ExtendWith(MockitoExtension.class)
class MaterialAlertServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialBatchRepository batchRepository;
    @Mock private MaterialAlertRecordRepository alertRecordRepository;
    @Mock private MaterialAlertRecordMapper alertRecordMapper;
    @Mock private MessagePublisher messagePublisher;
    @Mock private MaterialConfig config;

    @InjectMocks private MaterialAlertServiceImpl alertService;

    private MaterialDO material(Long id, String name, int stock, int safetyStock) {
        MaterialDO m = new MaterialDO();
        m.setId(id);
        m.setMaterialName(name);
        m.setCurrentStock(stock);
        m.setSafetyStock(safetyStock);
        return m;
    }

    private MaterialBatchDO batch(Long id, Long materialId, int remaining,
                                   LocalDate expiryDate, LocalDate stockInDate) {
        MaterialBatchDO b = new MaterialBatchDO();
        b.setId(id);
        b.setMaterialId(materialId);
        b.setBatchNo("BTC" + id);
        b.setRemaining(remaining);
        b.setExpiryDate(expiryDate);
        b.setStockInTime(stockInDate.atStartOfDay());
        b.setBatchStatus(1);
        return b;
    }

    // ==================== 低库存检测 ====================

    @Test
    void testCheckLowStock_Triggered() {
        MaterialDO m1 = material(1L, "试剂A", 5, 10);  // stock <= safety
        MaterialDO m2 = material(2L, "试剂B", 20, 10); // stock > safety
        when(materialRepository.findAllWithSafetyStock()).thenReturn(List.of(m1, m2));
        when(alertRecordRepository.existsTodayAlert(1L, null, 1, LocalDate.now())).thenReturn(false);

        alertService.checkLowStock();

        ArgumentCaptor<MaterialAlertRecordDO> captor = ArgumentCaptor.forClass(MaterialAlertRecordDO.class);
        verify(alertRecordRepository).insert(captor.capture());
        MaterialAlertRecordDO record = captor.getValue();
        assertEquals(1L, record.getMaterialId().longValue());
        assertEquals(1, record.getAlertType().intValue());
        assertTrue(record.getAlertContent().contains("试剂A"));
        verify(messagePublisher).publish(argThat(cmd ->
                cmd.getMessageType().equals("material.warning.low_stock")));
    }

    @Test
    void testCheckLowStock_NotTriggered() {
        MaterialDO m = material(1L, "试剂A", 20, 10); // stock > safety
        when(materialRepository.findAllWithSafetyStock()).thenReturn(List.of(m));

        alertService.checkLowStock();

        verify(alertRecordRepository, never()).insert(any());
        verify(messagePublisher, never()).publish(any());
    }

    @Test
    void testCheckLowStock_AlreadyAlertToday_Skip() {
        MaterialDO m = material(1L, "试剂A", 5, 10);
        when(materialRepository.findAllWithSafetyStock()).thenReturn(List.of(m));
        when(alertRecordRepository.existsTodayAlert(1L, null, 1, LocalDate.now())).thenReturn(true);

        alertService.checkLowStock();

        verify(alertRecordRepository, never()).insert(any());
        verify(messagePublisher, never()).publish(any());
    }

    @Test
    void testCheckLowStock_MultipleMaterials_PartialTrigger() {
        MaterialDO m1 = material(1L, "试剂A", 3, 10);   // 触发
        MaterialDO m2 = material(2L, "试剂B", 50, 10);  // 不触发
        MaterialDO m3 = material(3L, "试剂C", 8, 10);   // 触发
        when(materialRepository.findAllWithSafetyStock()).thenReturn(List.of(m1, m2, m3));
        when(alertRecordRepository.existsTodayAlert(1L, null, 1, LocalDate.now())).thenReturn(false);
        when(alertRecordRepository.existsTodayAlert(3L, null, 1, LocalDate.now())).thenReturn(false);

        alertService.checkLowStock();

        verify(alertRecordRepository, times(2)).insert(any());
    }

    // ==================== 临期检测 ====================

    @Test
    void testCheckExpiry_NearExpiry() {
        LocalDate today = LocalDate.now();
        when(config.getNearExpiryDays()).thenReturn(30);

        MaterialBatchDO nearBatch = batch(1L, 1L, 50, today.plusDays(15), today.minusDays(10));
        when(batchRepository.findAvailableByExpiryRange(today.minusDays(1), today.plusDays(30)))
                .thenReturn(List.of(nearBatch));
        when(alertRecordRepository.existsTodayAlert(1L, 1L, 2, today)).thenReturn(false);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "试剂A", 100, 10)));

        alertService.checkExpiry();

        ArgumentCaptor<MaterialAlertRecordDO> captor = ArgumentCaptor.forClass(MaterialAlertRecordDO.class);
        verify(alertRecordRepository).insert(captor.capture());
        assertEquals(2, captor.getValue().getAlertType().intValue());
        assertTrue(captor.getValue().getAlertContent().contains("BTC1"));
    }

    @Test
    void testCheckExpiry_Expired_AutoScrap() {
        LocalDate today = LocalDate.now();
        when(config.getNearExpiryDays()).thenReturn(30);

        MaterialBatchDO expiredBatch = batch(1L, 1L, 50, today.minusDays(1), today.minusDays(100));
        when(batchRepository.findAvailableByExpiryRange(today.minusDays(1), today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(batchRepository.findAvailableByExpiryRange(null, today.minusDays(1)))
                .thenReturn(List.of(expiredBatch));
        when(alertRecordRepository.existsTodayAlert(1L, 1L, 3, today)).thenReturn(false);
        // CR-1: CAS 扣减返回>0 才继续自动报废
        when(batchRepository.deductRemaining(1L, 50)).thenReturn(1);

        MaterialDO material = material(1L, "试剂A", 100, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));

        alertService.checkExpiry();

        // 过期批次：插入预警 + 自动报废
        verify(alertRecordRepository).insert(argThat(r -> r.getAlertType() == 3));
        verify(batchRepository).deductRemaining(1L, 50);
        verify(batchRepository).updateBatchStatus(1L, 3);
        verify(materialRepository).updateCurrentStock(1L, -50);
    }

    @Test
    void testCheckExpiry_Expired_NoStockAutoScrapStillWorks() {
        // 过期但余量0的批次不被 findAvailableByExpiryRange 查出（gt remaining>0）
        // 所以不会被处理——这是设计正确的行为
        LocalDate today = LocalDate.now();
        when(config.getNearExpiryDays()).thenReturn(30);
        when(batchRepository.findAvailableByExpiryRange(today.minusDays(1), today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(batchRepository.findAvailableByExpiryRange(null, today.minusDays(1)))
                .thenReturn(Collections.emptyList());

        alertService.checkExpiry();

        verify(alertRecordRepository, never()).insert(any());
        verify(batchRepository, never()).deductRemaining(anyLong(), anyInt());
    }

    @Test
    void testCheckExpiry_NearExpiry_AlreadyAlertToday_Skip() {
        LocalDate today = LocalDate.now();
        when(config.getNearExpiryDays()).thenReturn(30);

        MaterialBatchDO nearBatch = batch(1L, 1L, 50, today.plusDays(15), today.minusDays(10));
        when(batchRepository.findAvailableByExpiryRange(today.minusDays(1), today.plusDays(30)))
                .thenReturn(List.of(nearBatch));
        when(alertRecordRepository.existsTodayAlert(1L, 1L, 2, today)).thenReturn(true);

        alertService.checkExpiry();

        verify(alertRecordRepository, never()).insert(any());
    }

    // ==================== 超量领用预警 ====================

    @Test
    void testCreateOverReceiveAlert_AboveThreshold() {
        when(config.getOverReceiveThreshold()).thenReturn(100);
        when(materialRepository.findById(1L)).thenReturn(
                Optional.of(material(1L, "试剂A", 500, 10)));

        alertService.createOverReceiveAlert(1L, 150);

        ArgumentCaptor<MaterialAlertRecordDO> captor = ArgumentCaptor.forClass(MaterialAlertRecordDO.class);
        verify(alertRecordRepository).insert(captor.capture());
        assertEquals(4, captor.getValue().getAlertType().intValue());
        assertTrue(captor.getValue().getAlertContent().contains("150"));
    }

    @Test
    void testCreateOverReceiveAlert_BelowThreshold_NoAlert() {
        when(config.getOverReceiveThreshold()).thenReturn(100);

        alertService.createOverReceiveAlert(1L, 50);

        verify(alertRecordRepository, never()).insert(any());
        verify(materialRepository, never()).findById(any());
    }

    @Test
    void testCreateOverReceiveAlert_EqualToThreshold_NoAlert() {
        // threshold=100, quantity=100 → 不超过（条件为 >）
        when(config.getOverReceiveThreshold()).thenReturn(100);

        alertService.createOverReceiveAlert(1L, 100);

        verify(alertRecordRepository, never()).insert(any());
    }

    // ==================== 预警处理 ====================

    @Test
    void testHandleAlert_Normal() {
        MaterialAlertRecordDO record = new MaterialAlertRecordDO();
        record.setId(1L);
        record.setMaterialId(1L);
        record.setAlertType(1);
        record.setStatus(0);
        record.setAlertContent("低库存预警");
        when(alertRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        alertService.handleAlert(1L, 100L, "已采购补货");

        assertEquals(1, record.getStatus().intValue());
        assertEquals(100L, record.getHandledBy().longValue());
        assertNotNull(record.getHandledAt());
        assertTrue(record.getAlertContent().contains("已采购补货"));
        verify(alertRecordRepository).updateById(record);
    }

    @Test
    void testHandleAlert_NoResult() {
        MaterialAlertRecordDO record = new MaterialAlertRecordDO();
        record.setId(1L);
        record.setStatus(0);
        record.setAlertContent("低库存预警");
        when(alertRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        alertService.handleAlert(1L, 100L, null);
        // 不拼接 [处理结果:]
        assertFalse(record.getAlertContent().contains("处理结果"));
        verify(alertRecordRepository).updateById(record);
    }

    @Test
    void testHandleAlert_AlreadyHandled_ShouldThrow() {
        MaterialAlertRecordDO record = new MaterialAlertRecordDO();
        record.setId(1L);
        record.setStatus(1); // 已处理
        when(alertRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThrows(MaterialException.class,
                () -> alertService.handleAlert(1L, 100L, "再处理"));
    }

    @Test
    void testHandleAlert_NotFound() {
        when(alertRecordRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(MaterialException.class,
                () -> alertService.handleAlert(999L, 100L, "处理"));
    }

    // ==================== 预警分页查询 ====================

    @Test
    void testPageAlerts() {
        AlertQueryDTO query = new AlertQueryDTO();
        query.setMaterialId(1L);
        query.setAlertType(1);
        query.setPageNum(1);
        query.setPageSize(20);

        IPage<MaterialAlertRecordDO> pageResult = mock(IPage.class);
        when(alertRecordMapper.selectPage(isA(IPage.class), any())).thenReturn(pageResult);
        when(pageResult.getRecords()).thenReturn(Collections.emptyList());
        when(pageResult.getTotal()).thenReturn(0L);
        when(pageResult.getCurrent()).thenReturn(1L);
        when(pageResult.getSize()).thenReturn(20L);

        var result = alertService.pageAlerts(query);
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }
}
