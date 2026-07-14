package com.zhangjintao.project.smart_lab_backend.modules.material;

import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveLogVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.impl.MaterialReceiveServiceImpl;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 领用管理服务单元测试 — 7.1 MaterialReceiveServiceTest
 * <p>
 * 覆盖：申请校验、取消状态校验、审批FIFO选批（单批/多批/精确消耗/临期优先）、
 * 驳回记录原因、并发CAS冲突、超量预警触发、消息推送
 *
 * @author linbowei
 */
@ExtendWith(MockitoExtension.class)
class MaterialReceiveServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialBatchRepository batchRepository;
    @Mock private MaterialReceiveApplyRepository applyRepository;
    @Mock private MaterialReceiveLogRepository logRepository;
    @Mock private MaterialReceiveBatchDetailRepository detailRepository;
    @Mock private MaterialAlertService materialAlertService;
    @Mock private MessagePublisher messagePublisher;
    @Mock private PersonDirectoryPort personDirectoryPort;
    @Mock private MaterialConfig config;

    @InjectMocks private MaterialReceiveServiceImpl receiveService;

    private MaterialDO material(Long id, String name, int stock) {
        MaterialDO m = new MaterialDO();
        m.setId(id);
        m.setMaterialName(name);
        m.setCurrentStock(stock);
        return m;
    }

    private MaterialBatchDO batch(Long id, int remaining, LocalDate expiry, LocalDate stockIn) {
        MaterialBatchDO b = new MaterialBatchDO();
        b.setId(id);
        b.setBatchNo("BTC" + id);
        b.setRemaining(remaining);
        b.setExpiryDate(expiry);
        b.setStockInTime(stockIn.atStartOfDay());
        b.setBatchStatus(1);
        return b;
    }

    private MaterialReceiveApplyDO apply(Long id, String applyNo, Long materialId, int quantity, int status) {
        MaterialReceiveApplyDO a = new MaterialReceiveApplyDO();
        a.setId(id);
        a.setApplyNo(applyNo);
        a.setMaterialId(materialId);
        a.setApplicantId(10L);
        a.setExpectedQuantity(quantity);
        a.setApplyStatus(status);
        return a;
    }

    // ==================== FR-0003 领用申请 ====================

    @Test
    void testApply_Normal() {
        MaterialDO m = material(1L, "试剂A", 100);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(applyRepository.existsPendingApply(1L, 10L)).thenReturn(false);
        when(config.getApplyNoPrefix()).thenReturn("MRP");

        ReceiveApplyDTO dto = new ReceiveApplyDTO();
        dto.setMaterialId(1L);
        dto.setExpectedQuantity(10);
        dto.setPurpose("实验用");

        ReceiveApplyVO vo = receiveService.apply(dto, 10L);

        assertNotNull(vo);
        assertEquals("MRP", vo.getApplyNo().substring(0, 3));
        assertEquals(1, vo.getApplyStatus().intValue()); // 待审核
        verify(applyRepository).insert(any(MaterialReceiveApplyDO.class));
    }

    @Test
    void testApply_StockInsufficient() {
        MaterialDO m = material(1L, "试剂A", 5);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));

        ReceiveApplyDTO dto = new ReceiveApplyDTO();
        dto.setMaterialId(1L);
        dto.setExpectedQuantity(10);

        MaterialException ex = assertThrows(MaterialException.class,
                () -> receiveService.apply(dto, 10L));
        assertEquals(MaterialErrorCode.STOCK_INSUFFICIENT.getCode(), ex.getCode());
    }

    @Test
    void testApply_DuplicatePending() {
        MaterialDO m = material(1L, "试剂A", 100);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(applyRepository.existsPendingApply(1L, 10L)).thenReturn(true);

        ReceiveApplyDTO dto = new ReceiveApplyDTO();
        dto.setMaterialId(1L);
        dto.setExpectedQuantity(10);

        MaterialException ex = assertThrows(MaterialException.class,
                () -> receiveService.apply(dto, 10L));
        assertEquals(MaterialErrorCode.APPLY_DUPLICATE_PENDING.getCode(), ex.getCode());
    }

    @Test
    void testApply_MaterialNotFound() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        ReceiveApplyDTO dto = new ReceiveApplyDTO();
        dto.setMaterialId(999L);
        dto.setExpectedQuantity(10);

        assertThrows(MaterialException.class, () -> receiveService.apply(dto, 10L));
    }

    // ==================== FR-0003 取消申请 ====================

    @Test
    void testCancel_Normal() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 1);
        a.setApplicantId(10L);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));

        receiveService.cancel("MRP001", 10L);

        assertEquals(4, a.getApplyStatus().intValue()); // 已取消
        verify(applyRepository).updateById(a);
    }

    @Test
    void testCancel_NotOwner() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 1);
        a.setApplicantId(10L);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));

        MaterialException ex = assertThrows(MaterialException.class,
                () -> receiveService.cancel("MRP001", 20L));
        assertEquals(MaterialErrorCode.APPLY_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void testCancel_WrongStatus() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 2); // 已审批通过
        a.setApplicantId(10L);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));

        assertThrows(MaterialException.class,
                () -> receiveService.cancel("MRP001", 10L));
    }

    // ==================== FR-0004 审批出库：FIFO选批 ====================

    @Test
    void testApprove_SingleBatch_PartialDeduct() {
        // 单批100，申请50，扣减后批余量50（状态不变）
        MaterialDO m = material(1L, "试剂A", 100);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 1);
        MaterialBatchDO b1 = batch(1L, 100, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 7, 1));

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(batchRepository.findAvailableByMaterialId(1L)).thenReturn(List.of(b1));
        when(batchRepository.deductRemaining(1L, 50)).thenReturn(1);


        // resolveUserId 返回 null 则消息跳过，不 mock messagePublisher
        when(personDirectoryPort.getByPersonId(10L)).thenReturn(Optional.empty());

        ReceiveLogVO result = receiveService.approve("MRP001", 20L);

        assertNotNull(result);
        assertEquals(50, result.getTotalQuantity().intValue());
        assertEquals(20L, result.getOutOperatorId().longValue());

        verify(batchRepository).deductRemaining(1L, 50);
        verify(batchRepository, never()).updateBatchStatus(eq(1L), anyInt());
        verify(materialRepository).updateCurrentStock(1L, -50);
        verify(logRepository).insert(any(MaterialReceiveLogDO.class));
        verify(detailRepository).insertBatch(anyList());
        assertEquals(2, a.getApplyStatus().intValue()); // 审批通过
        verify(materialAlertService).createOverReceiveAlert(1L, 50);
    }

    @Test
    void testApprove_SingleBatch_ExactDeduct_BatchCleared() {
        // 单批50，申请50，精确消耗 → 批次状态变已清空
        MaterialDO m = material(1L, "试剂A", 50);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 1);
        MaterialBatchDO b1 = batch(1L, 50, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 7, 1));

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(batchRepository.findAvailableByMaterialId(1L)).thenReturn(List.of(b1));
        when(batchRepository.deductRemaining(1L, 50)).thenReturn(1);

        when(personDirectoryPort.getByPersonId(10L)).thenReturn(Optional.empty());

        receiveService.approve("MRP001", 20L);

        // deduct == batch.remaining → batch status changed to 2 (清空)
        verify(batchRepository).updateBatchStatus(1L, 2);
    }

    @Test
    void testApprove_MultiBatch_EmptyFirstTwo() {
        // 批1(30) + 批2(40) + 批3(30)，申请80 → 批1清空、批2清空、批3扣10
        MaterialDO m = material(1L, "试剂A", 100);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 80, 1);

        MaterialBatchDO b1 = batch(1L, 30, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1));
        MaterialBatchDO b2 = batch(2L, 40, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 7, 5));
        MaterialBatchDO b3 = batch(3L, 30, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 7, 10));

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(batchRepository.findAvailableByMaterialId(1L)).thenReturn(List.of(b1, b2, b3));
        when(batchRepository.deductRemaining(1L, 30)).thenReturn(1);
        when(batchRepository.deductRemaining(2L, 40)).thenReturn(1);
        when(batchRepository.deductRemaining(3L, 10)).thenReturn(1);

        when(personDirectoryPort.getByPersonId(10L)).thenReturn(Optional.empty());

        receiveService.approve("MRP001", 20L);

        verify(batchRepository).updateBatchStatus(1L, 2); // 清空
        verify(batchRepository).updateBatchStatus(2L, 2); // 清空
        verify(batchRepository, never()).updateBatchStatus(eq(3L), anyInt()); // 剩20，未清空
    }

    @Test
    void testApprove_SortByExpiry_NearExpiryFirst() {
        // 批1(12月到期) + 批2(3月到期)，申请50 → 应选批2（临期优先不是入库顺序FIFO）
        MaterialDO m = material(1L, "试剂A", 100);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 1);

        // 批1先入库但有效期晚，批2后入库但有效期早
        MaterialBatchDO b1 = batch(1L, 50, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 7, 1));
        MaterialBatchDO b2 = batch(2L, 50, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 5));

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        // findAvailableByMaterialId ORDER BY expiry_date ASC, stock_in_time ASC
        when(batchRepository.findAvailableByMaterialId(1L)).thenReturn(List.of(b2, b1));
        when(batchRepository.deductRemaining(2L, 50)).thenReturn(1);

        when(personDirectoryPort.getByPersonId(10L)).thenReturn(Optional.empty());

        receiveService.approve("MRP001", 20L);

        // 只扣了b2, b1从未扣减
        verify(batchRepository).deductRemaining(2L, 50);
        verify(batchRepository, never()).deductRemaining(eq(1L), anyInt());
    }

    @Test
    void testApprove_InsufficientStock_ShouldThrow() {
        MaterialDO m = material(1L, "试剂A", 30);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 1);

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));

        MaterialException ex = assertThrows(MaterialException.class,
                () -> receiveService.approve("MRP001", 20L));
        assertEquals(MaterialErrorCode.STOCK_INSUFFICIENT.getCode(), ex.getCode());
    }

    @Test
    void testApprove_NotPending_ShouldThrow() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 2); // 已审批
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));

        assertThrows(MaterialException.class,
                () -> receiveService.approve("MRP001", 20L));
    }

    @Test
    void testApprove_CASConflict_ShouldThrow() {
        MaterialDO m = material(1L, "试剂A", 50);
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 50, 1);
        MaterialBatchDO b1 = batch(1L, 50, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 7, 1));

        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(batchRepository.findAvailableByMaterialId(1L)).thenReturn(List.of(b1));
        when(batchRepository.deductRemaining(1L, 50)).thenReturn(0); // CAS失败

        MaterialException ex = assertThrows(MaterialException.class,
                () -> receiveService.approve("MRP001", 20L));
        assertEquals(MaterialErrorCode.STOCK_INSUFFICIENT.getCode(), ex.getCode());
    }

    // ==================== FR-0004 驳回 ====================

    @Test
    void testReject_Normal() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 1);
        a.setApplicantId(10L);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(personDirectoryPort.getByPersonId(10L)).thenReturn(Optional.empty());
        // 查找物资名称用于消息
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "试剂A", 100)));

        receiveService.reject("MRP001", 20L, "库存不足");

        assertEquals(3, a.getApplyStatus().intValue()); // 已驳回
        assertEquals(20L, a.getApproverId().longValue());
        assertEquals("库存不足", a.getRejectReason());
        verify(applyRepository).updateById(a);
    }

    @Test
    void testReject_AlreadyApproved() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 2);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));

        assertThrows(MaterialException.class,
                () -> receiveService.reject("MRP001", 20L, "no"));
    }

    @Test
    void testReject_SendsMessage() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 1);
        a.setApplicantId(10L);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "试剂A", 100)));
        when(personDirectoryPort.getByPersonId(10L))
                .thenReturn(Optional.of(PersonDirectoryItemDTO.builder().userId(999L).build()));

        receiveService.reject("MRP001", 20L, "无库存");

        verify(messagePublisher).publish(argThat(cmd ->
                cmd.getMessageType().equals("material.normal.receive.rejected")
                && cmd.getTargetUserIds().contains(999L)
        ));
    }

    // ==================== FR-0005 详情 ====================

    @Test
    void testGetReceiveDetail_ApprovedWithLog() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 2);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "试剂A", 100)));
        when(applyRepository.findById(1L)).thenReturn(Optional.of(a));

        MaterialReceiveLogDO log = new MaterialReceiveLogDO();
        log.setId(100L);
        log.setApplyId(1L);
        log.setTotalQuantity(10);
        when(logRepository.findByApplyId(1L)).thenReturn(Optional.of(log));
        when(detailRepository.findByReceiveLogId(100L)).thenReturn(List.of());

        var detail = receiveService.getReceiveDetail("MRP001");

        assertNotNull(detail.getReceiveLog());
        assertEquals(10, detail.getReceiveLog().getTotalQuantity().intValue());
    }

    @Test
    void testGetReceiveDetail_PendingNoLog() {
        MaterialReceiveApplyDO a = apply(1L, "MRP001", 1L, 10, 1);
        when(applyRepository.findByApplyNo("MRP001")).thenReturn(Optional.of(a));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, "试剂A", 100)));

        var detail = receiveService.getReceiveDetail("MRP001");

        assertNotNull(detail.getApply());
        assertNull(detail.getReceiveLog());
    }
}
