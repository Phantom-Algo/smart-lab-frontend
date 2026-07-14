package com.zhangjintao.project.smart_lab_backend.modules.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.enums.BatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialBatchService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 物资管理服务实现 — FR-0001/FR-0002
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository materialBatchRepository;
    private final MaterialStockInRepository stockInRepository;
    private final MaterialReceiveApplyRepository materialReceiveApplyRepository;
    private final MaterialMapper materialMapper;
    private final MaterialConfig materialConfig;
    private final MaterialBatchService materialBatchService;
    private final RealtimePublisher realtimePublisher;

    /** 当日自增序列（避免同毫秒编号碰撞） */
    private final AtomicInteger dailySeq = new AtomicInteger(1);
    private volatile String dailySeqDate = "";

    // ==================== FR-0001 物资台账查询 ====================

    @Override
    public PageResult<MaterialVO> pageMaterials(MaterialQueryDTO query) {
        Page<MaterialDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<MaterialDO> result = materialRepository.pageByCondition(
                query.getMaterialName(),
                query.getLabId(),
                query.getMaterialNo(),
                page);
        // N+1 优化：批量加载所有物资的批次数据
        List<MaterialDO> materials = result.getRecords();
        Map<Long, List<MaterialBatchDO>> batchesByMaterial = Collections.emptyMap();
        if (!materials.isEmpty()) {
            List<Long> materialIds = materials.stream().map(MaterialDO::getId).collect(Collectors.toList());
            List<MaterialBatchDO> allBatches = materialBatchRepository.findByMaterialIds(materialIds);
            batchesByMaterial = allBatches.stream()
                    .collect(Collectors.groupingBy(MaterialBatchDO::getMaterialId));
        }
        Map<Long, List<MaterialBatchDO>> finalBatches = batchesByMaterial;
        List<MaterialVO> vos = materials.stream()
                .map(m -> toMaterialVO(m, finalBatches.getOrDefault(m.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MaterialDetailVO getMaterialDetail(Long id) {
        MaterialDO material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        List<MaterialBatchDO> batches = materialBatchRepository.findByMaterialId(id);
        List<MaterialBatchVO> batchVOs = batches.stream()
                .map(materialBatchService::toBatchVO)
                .collect(Collectors.toList());
        return MaterialDetailVO.builder()
                .id(material.getId())
                .materialNo(material.getMaterialNo())
                .materialName(material.getMaterialName())
                .labId(material.getLabId())
                .unit(material.getUnit())
                .safetyStock(material.getSafetyStock())
                .currentStock(material.getCurrentStock())
                .batches(batchVOs)
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    // ==================== FR-0002 物资信息维护 ====================

    @Override
    @Transactional
    public MaterialVO createMaterial(MaterialCreateDTO dto, Long operatorId) {
        String materialNo = generateMaterialNo();
        // 编号来自 DB 自增 max+1，正常情况下不会重复；DB UNIQUE KEY 是最终防线
        MaterialDO material = new MaterialDO();
        material.setMaterialNo(materialNo);
        material.setMaterialName(dto.getMaterialName());
        material.setLabId(dto.getLabId());
        material.setUnit(dto.getUnit());
        material.setSafetyStock(dto.getSafetyStock());
        material.setCurrentStock(0);
        materialRepository.insert(material);
        publishRealtime("material.created", material, Map.of());
        log.info("物资创建成功，id={}, materialNo={}, operatorId={}", material.getId(), materialNo, operatorId);
        return toMaterialVO(material);
    }

    @Override
    @Transactional
    public MaterialVO updateMaterial(Long id, MaterialUpdateDTO dto) {
        MaterialDO material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        if (dto.getMaterialName() != null) material.setMaterialName(dto.getMaterialName());
        if (dto.getLabId() != null) material.setLabId(dto.getLabId());
        if (dto.getUnit() != null) material.setUnit(dto.getUnit());
        if (dto.getSafetyStock() != null) material.setSafetyStock(dto.getSafetyStock());
        materialRepository.updateById(material);
        publishRealtime("material.info_updated", material, Map.of());
        log.info("物资更新成功，id={}", id);
        return toMaterialVO(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long id) {
        MaterialDO material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        List<MaterialBatchDO> batches = materialBatchRepository.findByMaterialId(id);
        boolean hasStock = batches.stream().anyMatch(b -> b.getRemaining() > 0);
        if (hasStock) {
            throw new MaterialException(MaterialErrorCode.MATERIAL_HAS_STOCK, "物资尚有库存，无法删除");
        }
        if (materialReceiveApplyRepository.existsPendingByMaterialId(id)) {
            throw new MaterialException(MaterialErrorCode.MATERIAL_HAS_PENDING_APPLY, "物资有进行中的领用申请，无法删除");
        }
        materialRepository.deleteById(id);
        publishRealtime("material.deleted", material, Map.of());
        log.info("物资删除成功，id={}, materialNo={}", id, material.getMaterialNo());
    }

    @Override
    @Transactional
    public StockInResultVO stockIn(StockInDTO dto, Long operatorId) {
        // 1. 校验物资存在
        MaterialDO material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        // 2. 生成批次编号、创建批次
        String batchNo = generateBatchNo();
        MaterialBatchDO batch = new MaterialBatchDO();
        batch.setBatchNo(batchNo);
        batch.setMaterialId(dto.getMaterialId());
        batch.setQuantity(dto.getQuantity());
        batch.setRemaining(dto.getQuantity());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setStockInTime(LocalDateTime.now());
        batch.setBatchStatus(1); // 在库可用
        batch.setOperatorId(operatorId);
        batch.setRemark(dto.getRemark());
        materialBatchRepository.insert(batch);
        // 3. 生成入库单号、入库记录
        String stockInNo = generateStockInNo();
        MaterialStockInDO stockIn = new MaterialStockInDO();
        stockIn.setStockInNo(stockInNo);
        stockIn.setBatchId(batch.getId());
        stockIn.setMaterialId(dto.getMaterialId());
        stockIn.setQuantity(dto.getQuantity());
        stockIn.setOperatorId(operatorId);
        stockIn.setRemark(dto.getRemark());
        stockInRepository.insert(stockIn);
        // 4. 更新物资库存
        materialRepository.updateCurrentStock(material.getId(), dto.getQuantity());
        // 重新查询最新库存（防止事务内旧值）
        MaterialDO updatedMaterial = materialRepository.findById(dto.getMaterialId()).orElse(material);
        publishRealtime("material.stock_inbound", updatedMaterial, Map.of(
            "batchNo", batchNo,
            "stockInNo", stockInNo,
            "quantity", dto.getQuantity(),
            "expiryDate", dto.getExpiryDate(),
            "remark", dto.getRemark()));
        publishRealtime("material.stock_changed", updatedMaterial, Map.of(
            "batchNo", batchNo,
            "delta", dto.getQuantity(),
            "reason", "stock_in"));
        log.info("入库成功，materialId={}, batchNo={}, stockInNo={}, quantity={}",
                dto.getMaterialId(), batchNo, stockInNo, dto.getQuantity());
        return StockInResultVO.builder()
                .batchNo(batchNo)
                .stockInNo(stockInNo)
                .quantity(dto.getQuantity())
                .newCurrentStock(updatedMaterial.getCurrentStock())
                .build();
    }

    // ========== 编号生成 ==========

    private String generateMaterialNo() {
        String prefix = materialConfig.getMaterialNoPrefix()
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用 selectPage + size=1 替代 MySQL 专有 .last("LIMIT 1")，兼容 H2
        List<MaterialDO> materials = materialMapper.selectPage(
                new Page<>(1, 1),
                new LambdaQueryWrapper<MaterialDO>()
                        .likeRight(MaterialDO::getMaterialNo, prefix)
                        .orderByDesc(MaterialDO::getMaterialNo))
                .getRecords();
        int seq = 1;
        if (!materials.isEmpty()) {
            String maxNo = materials.get(0).getMaterialNo();
            String seqPart = maxNo.substring(prefix.length());
            try { seq = Integer.parseInt(seqPart) + 1; }
            catch (NumberFormatException e) { log.warn("物资编号流水号解析失败，maxNo={}", maxNo); }
        }
        return prefix + String.format("%04d", seq);
    }

    private String generateBatchNo() {
        return generateDailySeqNo(materialConfig.getBatchNoPrefix());
    }

    private String generateStockInNo() {
        return generateDailySeqNo(materialConfig.getStockInNoPrefix());
    }

    /** 按日自增编号：{prefix}{yyyyMMdd}{4位流水} */
    private String generateDailySeqNo(String prefix) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(dailySeqDate)) {
            dailySeqDate = today;
            dailySeq.set(1);
        }
        return prefix + today + String.format("%04d", dailySeq.getAndIncrement());
    }

    // ========== DO → VO ==========

    /** 单物资转VO（内部通过 findById 查批次，仅用于单一物资场景） */
    private MaterialVO toMaterialVO(MaterialDO material) {
        return toMaterialVO(material, materialBatchRepository.findByMaterialId(material.getId()));
    }

    /** 批量转VO（接受预加载的批次数据，用于分页 N+1 优化） */
    private MaterialVO toMaterialVO(MaterialDO material, List<MaterialBatchDO> batches) {
        LocalDate earliestExpiry = batches.stream()
                .map(MaterialBatchDO::getExpiryDate)
                .min(Comparator.naturalOrder()).orElse(null);
        String earliestBatchNo = batches.stream()
                .filter(b -> b.getExpiryDate() != null)
                .min(Comparator.comparing(MaterialBatchDO::getExpiryDate)
                        .thenComparing(MaterialBatchDO::getStockInTime))
                .map(MaterialBatchDO::getBatchNo).orElse(null);
        return MaterialVO.builder()
                .id(material.getId()).materialNo(material.getMaterialNo())
                .materialName(material.getMaterialName()).labId(material.getLabId())
                .unit(material.getUnit()).safetyStock(material.getSafetyStock())
                .currentStock(material.getCurrentStock()).batchCount(batches.size())
                .earliestExpiryDate(earliestExpiry).earliestBatchNo(earliestBatchNo)
                .createdAt(material.getCreatedAt()).updatedAt(material.getUpdatedAt())
                .build();
    }

    private void publishRealtime(String eventName, MaterialDO material, Map<String, Object> extraData) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("materialId", material.getId());
        payload.put("materialNo", material.getMaterialNo());
        payload.put("materialName", material.getMaterialName());
        payload.put("labId", material.getLabId());
        payload.put("unit", material.getUnit());
        payload.put("safetyStock", material.getSafetyStock());
        payload.put("currentStock", material.getCurrentStock());
        payload.putAll(extraData);
        realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
                .event(eventName)
                .source("material")
                .targetType(RealtimeTargetType.BROADCAST)
                .data(payload)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
