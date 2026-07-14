package com.zhangjintao.project.smart_lab_backend.modules.material;

import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialStockInDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialBatchRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialReceiveApplyRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialStockInRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.StockInDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.StockInResultVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialBatchService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.impl.MaterialServiceImpl;
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
 * 物资管理服务单元测试 — 7.1 MaterialServiceTest
 *
 * @author linbowei
 */
@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialBatchRepository materialBatchRepository;
    @Mock private MaterialStockInRepository stockInRepository;
    @Mock private MaterialReceiveApplyRepository materialReceiveApplyRepository;
    @Mock private MaterialMapper materialMapper;
    @Mock private MaterialConfig materialConfig;
    @Mock private MaterialBatchService materialBatchService;

    @InjectMocks private MaterialServiceImpl materialService;

    private MaterialDO createMaterial(Long id, String name, int stock, int safetyStock) {
        MaterialDO m = new MaterialDO();
        m.setId(id);
        m.setMaterialNo("MTL" + id);
        m.setMaterialName(name);
        m.setLabId(1L);
        m.setUnit("瓶");
        m.setSafetyStock(safetyStock);
        m.setCurrentStock(stock);
        return m;
    }

    // ==================== FR-0001 分页查询 ====================

    @Test
    @SuppressWarnings("unchecked")
    void testPageMaterials_WithConditions() {
        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setMaterialName("试剂");
        query.setLabId(1L);
        query.setPageNum(1);
        query.setPageSize(20);

        IPage<MaterialDO> pageResult = mock(IPage.class);
        MaterialDO m = createMaterial(1L, "测试试剂", 100, 10);
        when(materialRepository.pageByCondition(eq("试剂"), eq(1L), isNull(), any()))
                .thenReturn(pageResult);
        when(pageResult.getRecords()).thenReturn(List.of(m));
        when(pageResult.getTotal()).thenReturn(1L);
        when(pageResult.getCurrent()).thenReturn(1L);
        when(pageResult.getSize()).thenReturn(20L);
        // N+1 优化：批量加载批次数据
        when(materialBatchRepository.findByMaterialIds(anyList())).thenReturn(Collections.emptyList());

        var result = materialService.pageMaterials(query);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("测试试剂", result.getRecords().get(0).getMaterialName());
        verify(materialRepository).pageByCondition(eq("试剂"), eq(1L), isNull(), any());
    }

    // ==================== FR-0001 详情 ====================

    @Test
    void testGetMaterialDetail_WithBatches() {
        MaterialDO m = createMaterial(1L, "试剂A", 200, 20);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));

        MaterialBatchDO batch1 = new MaterialBatchDO();
        batch1.setId(10L);
        batch1.setBatchNo("BTC001");
        batch1.setRemaining(100);
        batch1.setExpiryDate(LocalDate.of(2026, 12, 31));
        MaterialBatchDO batch2 = new MaterialBatchDO();
        batch2.setId(11L);
        batch2.setBatchNo("BTC002");
        batch2.setRemaining(100);
        batch2.setExpiryDate(LocalDate.of(2027, 6, 30));
        when(materialBatchRepository.findByMaterialId(1L)).thenReturn(List.of(batch1, batch2));

        var detail = materialService.getMaterialDetail(1L);

        assertNotNull(detail);
        assertEquals("试剂A", detail.getMaterialName());
        assertEquals(200, detail.getCurrentStock());
        verify(materialBatchRepository).findByMaterialId(1L);
    }

    @Test
    void testGetMaterialDetail_NotFound() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(MaterialException.class, () -> materialService.getMaterialDetail(999L));
    }

    // ==================== FR-0002 创建物资 ====================

    @Test
    void testCreateMaterial_ShouldGenerateNoAndInsert() {
        when(materialConfig.getMaterialNoPrefix()).thenReturn("MTL");
        // generateMaterialNo 使用 selectPage 替代 MySQL 专有 LIMIT 1
        when(materialMapper.selectPage(any(), any())).thenReturn(new Page<>());

        MaterialCreateDTO dto = new MaterialCreateDTO();
        dto.setMaterialName("新试剂");
        dto.setLabId(1L);
        dto.setUnit("瓶");
        dto.setSafetyStock(10);

        MaterialVO vo = materialService.createMaterial(dto, 100L);

        assertNotNull(vo);
        assertTrue(vo.getMaterialNo().startsWith("MTL"));
        assertEquals("新试剂", vo.getMaterialName());
        assertEquals(0, vo.getCurrentStock());

        ArgumentCaptor<MaterialDO> captor = ArgumentCaptor.forClass(MaterialDO.class);
        verify(materialRepository).insert(captor.capture());
        MaterialDO inserted = captor.getValue();
        assertEquals("新试剂", inserted.getMaterialName());
        assertEquals(0, inserted.getCurrentStock());
        assertEquals(10, inserted.getSafetyStock());
    }

    // ==================== FR-0002 更新物资 ====================

    @Test
    void testUpdateMaterial_ShouldUpdateFields() {
        MaterialDO m = createMaterial(1L, "旧名", 100, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(materialBatchRepository.findByMaterialId(1L)).thenReturn(Collections.emptyList());

        MaterialUpdateDTO dto = new MaterialUpdateDTO();
        dto.setMaterialName("新名");
        dto.setSafetyStock(20);

        MaterialVO vo = materialService.updateMaterial(1L, dto);

        assertEquals("新名", vo.getMaterialName());
        verify(materialRepository).updateById(m);
        assertEquals("新名", m.getMaterialName());
        assertEquals(20, m.getSafetyStock());
    }

    // ==================== FR-0002 删除物资 ====================

    @Test
    void testDeleteMaterial_HasStock_ShouldThrow() {
        MaterialDO m = createMaterial(1L, "试剂", 50, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));

        MaterialBatchDO batch = new MaterialBatchDO();
        batch.setRemaining(50);
        when(materialBatchRepository.findByMaterialId(1L)).thenReturn(List.of(batch));

        MaterialException ex = assertThrows(MaterialException.class,
                () -> materialService.deleteMaterial(1L));
        assertEquals(MaterialErrorCode.MATERIAL_HAS_STOCK.getCode(), ex.getCode());
        verify(materialRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteMaterial_HasPendingApply_ShouldThrow() {
        MaterialDO m = createMaterial(1L, "试剂", 0, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(materialBatchRepository.findByMaterialId(1L)).thenReturn(Collections.emptyList());
        when(materialReceiveApplyRepository.existsPendingByMaterialId(1L)).thenReturn(true);

        MaterialException ex = assertThrows(MaterialException.class,
                () -> materialService.deleteMaterial(1L));
        assertEquals(MaterialErrorCode.MATERIAL_HAS_PENDING_APPLY.getCode(), ex.getCode());
        verify(materialRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteMaterial_Ok() {
        MaterialDO m = createMaterial(1L, "试剂", 0, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));
        when(materialBatchRepository.findByMaterialId(1L)).thenReturn(Collections.emptyList());
        when(materialReceiveApplyRepository.existsPendingByMaterialId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> materialService.deleteMaterial(1L));
        verify(materialRepository).deleteById(1L);
    }

    // ==================== FR-0002 入库 ====================

    @Test
    void testStockIn_ShouldCreateBatchAndUpdateStock() {
        when(materialConfig.getBatchNoPrefix()).thenReturn("BTC");
        when(materialConfig.getStockInNoPrefix()).thenReturn("MSI");

        MaterialDO m = createMaterial(1L, "试剂A", 100, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m));

        // 入库后重新查询返回更新的库存
        MaterialDO updated = createMaterial(1L, "试剂A", 200, 10);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(m), Optional.of(updated));

        StockInDTO dto = new StockInDTO();
        dto.setMaterialId(1L);
        dto.setQuantity(100);
        dto.setExpiryDate(LocalDate.of(2027, 6, 30));

        StockInResultVO result = materialService.stockIn(dto, 10L);

        assertNotNull(result);
        assertEquals(100, result.getQuantity());
        assertEquals(200, result.getNewCurrentStock());
        assertTrue(result.getBatchNo().startsWith("BTC"));
        assertTrue(result.getStockInNo().startsWith("MSI"));

        ArgumentCaptor<MaterialBatchDO> batchCaptor = ArgumentCaptor.forClass(MaterialBatchDO.class);
        verify(materialBatchRepository).insert(batchCaptor.capture());
        assertEquals(100, batchCaptor.getValue().getQuantity().intValue());
        assertEquals(100, batchCaptor.getValue().getRemaining().intValue());
        assertEquals(10L, batchCaptor.getValue().getOperatorId().longValue());

        verify(stockInRepository).insert(any(MaterialStockInDO.class));
        verify(materialRepository).updateCurrentStock(1L, 100);
    }

    @Test
    void testStockIn_MaterialNotFound() {
        when(materialRepository.findById(999L)).thenReturn(Optional.empty());

        StockInDTO dto = new StockInDTO();
        dto.setMaterialId(999L);
        dto.setQuantity(10);

        assertThrows(MaterialException.class, () -> materialService.stockIn(dto, 1L));
    }
}
