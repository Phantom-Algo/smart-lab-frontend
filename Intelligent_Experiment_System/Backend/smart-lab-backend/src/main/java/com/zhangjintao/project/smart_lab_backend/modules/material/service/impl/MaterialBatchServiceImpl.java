package com.zhangjintao.project.smart_lab_backend.modules.material.service.impl;

import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialBatchRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialRepository;
import com.zhangjintao.project.smart_lab_backend.modules.material.enums.BatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialBatchVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 批次管理服务实现 — FR-0006
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialBatchServiceImpl implements MaterialBatchService {

    private final MaterialBatchRepository batchRepository;
    private final MaterialRepository materialRepository;
    private final RealtimePublisher realtimePublisher;

    @Override
    public List<MaterialBatchVO> listBatchesByMaterial(Long materialId) {
        List<MaterialBatchDO> batches = batchRepository.findByMaterialId(materialId);
        return batches.stream()
                .map(this::toBatchVO)
                .collect(Collectors.toList());
    }

    private static final int UPDATE_SUCCESS = 1;

    @Override
    @Transactional
    public void scrapBatch(Long batchId, Long operatorId) {
        MaterialBatchDO batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.BATCH_NOT_FOUND));
        if (batch.getBatchStatus() != 1) {
            throw new MaterialException(MaterialErrorCode.BATCH_NOT_AVAILABLE);
        }
        // 1. 先 CAS 扣减余量为0（校验当前余量 = batch.getRemaining()，防止并发修改）
        int affected = batchRepository.deductRemaining(batchId, batch.getRemaining());
        if (affected < UPDATE_SUCCESS) {
            throw new MaterialException(MaterialErrorCode.BATCH_NOT_AVAILABLE, "批次余量已被并发修改，请重试");
        }
        // 2. 更新批次状态为报废
        batchRepository.updateBatchStatus(batchId, 3);
        // 3. 回收库存
        if (batch.getRemaining() > 0) {
            materialRepository.updateCurrentStock(batch.getMaterialId(), -batch.getRemaining());
        }
        MaterialDO material = materialRepository.findById(batch.getMaterialId()).orElse(null);
        if (material != null) {
            publishRealtime("material.batch_scrapped", material, batch);
            publishRealtime("material.stock_changed", material, batch);
        }
        log.info("批次已报废，batchId={}, materialId={}, remaining={}",
                batchId, batch.getMaterialId(), batch.getRemaining());
    }

    // ========== 辅助方法 ==========

    public MaterialBatchVO toBatchVO(MaterialBatchDO batch) {
        MaterialDO material = materialRepository.findById(batch.getMaterialId()).orElse(null);
        BatchStatus status = BatchStatus.fromCode(batch.getBatchStatus());
        return MaterialBatchVO.builder()
                .id(batch.getId())
                .batchNo(batch.getBatchNo())
                .materialId(batch.getMaterialId())
                .materialName(material != null ? material.getMaterialName() : null)
                .quantity(batch.getQuantity())
                .remaining(batch.getRemaining())
                .expiryDate(batch.getExpiryDate())
                .stockInTime(batch.getStockInTime())
                .batchStatus(batch.getBatchStatus())
                .batchStatusDesc(status != null ? status.getDesc() : null)
                .operatorId(batch.getOperatorId())
                .remark(batch.getRemark())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    private void publishRealtime(String eventName, MaterialDO material, MaterialBatchDO batch) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("materialId", material.getId());
        payload.put("materialNo", material.getMaterialNo());
        payload.put("materialName", material.getMaterialName());
        payload.put("currentStock", material.getCurrentStock());
        payload.put("batchId", batch.getId());
        payload.put("batchNo", batch.getBatchNo());
        payload.put("remaining", batch.getRemaining());
        payload.put("batchStatus", batch.getBatchStatus());
        payload.put("reason", "batch_scrapped");
        realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
                .event(eventName)
                .source("material")
                .targetType(RealtimeTargetType.BROADCAST)
                .data(payload)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
