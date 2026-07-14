package com.zhangjintao.project.smart_lab_backend.modules.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialAlertRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.enums.AlertStatus;
import com.zhangjintao.project.smart_lab_backend.modules.material.enums.AlertType;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialAlertVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预警管理服务实现 — FR-0007/FR-0008
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAlertServiceImpl implements MaterialAlertService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialAlertRecordRepository alertRecordRepository;
    private final MaterialAlertRecordMapper alertRecordMapper;
    private final MessagePublisher messagePublisher;
    private final MaterialConfig config;

    @Override
    @Transactional
    public void checkLowStock() {
        // 查询所有 safety_stock > 0 的物资
        List<MaterialDO> materials = materialRepository.findAllWithSafetyStock();
        // 在 Java 内存中过滤 current_stock <= safety_stock
        LocalDate today = LocalDate.now();
        for (MaterialDO material : materials) {
            if (material.getCurrentStock() <= material.getSafetyStock()) {
                if (alertRecordRepository.existsTodayAlert(material.getId(), null, 1, today)) {
                    continue;
                }
                MaterialAlertRecordDO record = buildAlertRecord(material.getId(), null, 1);
                record.setAlertContent(String.format("物资[%s]当前库存%d，已低于安全库存%d",
                        material.getMaterialName(), material.getCurrentStock(), material.getSafetyStock()));
                alertRecordRepository.insert(record);
                sendWarningMessage("material.warning.low_stock",
                        material.getMaterialName(), null, null, null);
                log.info("低库存预警生成，materialId={}, currentStock={}, safetyStock={}",
                        material.getId(), material.getCurrentStock(), material.getSafetyStock());
            }
        }
    }

    @Override
    @Transactional
    public void checkExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryDate = today.plusDays(config.getNearExpiryDays());

        // 临期检测：expiry_date 在 [today, nearExpiryDate] 且剩余>0 且在库
        List<MaterialBatchDO> nearBatches = batchRepository.findAvailableByExpiryRange(today.minusDays(1), nearExpiryDate);
        for (MaterialBatchDO batch : nearBatches) {
            if (alertRecordRepository.existsTodayAlert(batch.getMaterialId(), batch.getId(), 2, today)) {
                continue;
            }
            MaterialAlertRecordDO record = buildAlertRecord(batch.getMaterialId(), batch.getId(), 2);
            MaterialDO material = materialRepository.findById(batch.getMaterialId()).orElse(null);
            record.setAlertContent(String.format("批次[%s]有效期至%s，距临期不足%d天",
                    batch.getBatchNo(), batch.getExpiryDate(), config.getNearExpiryDays()));
            alertRecordRepository.insert(record);
            sendWarningMessage("material.warning.near_expiry",
                    material != null ? material.getMaterialName() : null,
                    batch.getBatchNo(), batch.getExpiryDate(), null);
        }

        // 过期检测：expiry_date < today 且剩余>0 且在库
        List<MaterialBatchDO> expiredBatches = batchRepository.findAvailableByExpiryRange(null, today.minusDays(1));
        for (MaterialBatchDO batch : expiredBatches) {
            if (alertRecordRepository.existsTodayAlert(batch.getMaterialId(), batch.getId(), 3, today)) {
                continue;
            }
            MaterialAlertRecordDO record = buildAlertRecord(batch.getMaterialId(), batch.getId(), 3);
            record.setAlertContent(String.format("批次[%s]已于%s过期", batch.getBatchNo(), batch.getExpiryDate()));
            alertRecordRepository.insert(record);
            // 过期自动报废：先清空余量(与scrapBatch行为一致)，再改状态
            int affected = batchRepository.deductRemaining(batch.getId(), batch.getRemaining());
            if (affected == 0) {
                log.warn("过期批次已被并发操作消耗，跳过自动报废。batchId={}", batch.getId());
                continue;
            }
            batchRepository.updateBatchStatus(batch.getId(), 3);
            MaterialDO material = materialRepository.findById(batch.getMaterialId()).orElse(null);
            if (material != null) {
                materialRepository.updateCurrentStock(material.getId(), -batch.getRemaining());
            }
            sendWarningMessage("material.warning.expired",
                    material != null ? material.getMaterialName() : null,
                    batch.getBatchNo(), batch.getExpiryDate(), null);
        }
    }

    @Override
    public void createOverReceiveAlert(Long materialId, int quantity) {
        if (quantity <= config.getOverReceiveThreshold()) {
            return;
        }
        MaterialDO material = materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        MaterialAlertRecordDO record = buildAlertRecord(materialId, null, 4);
        record.setAlertContent(String.format("物资[%s]单次领用%d，超过阈值%d",
                material.getMaterialName(), quantity, config.getOverReceiveThreshold()));
        alertRecordRepository.insert(record);
        sendWarningMessage("material.warning.over_receive",
                material.getMaterialName(), null, null, quantity);
    }

    @Override
    public PageResult<MaterialAlertVO> pageAlerts(AlertQueryDTO query) {
        Page<MaterialAlertRecordDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MaterialAlertRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getMaterialId() != null, MaterialAlertRecordDO::getMaterialId, query.getMaterialId())
               .eq(query.getAlertType() != null, MaterialAlertRecordDO::getAlertType, query.getAlertType())
               .eq(query.getStatus() != null, MaterialAlertRecordDO::getStatus, query.getStatus())
               .orderByDesc(MaterialAlertRecordDO::getAlertTime);
        IPage<MaterialAlertRecordDO> result = alertRecordMapper.selectPage(page, wrapper);
        List<MaterialAlertVO> vos = result.getRecords().stream()
                .map(this::toAlertVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void handleAlert(Long id, Long handlerId, String result) {
        MaterialAlertRecordDO record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.ALERT_NOT_FOUND));
        if (record.getStatus() != 0) {
            throw new MaterialException(MaterialErrorCode.ALERT_NOT_FOUND, "预警已处理");
        }
        record.setStatus(1);
        record.setHandledBy(handlerId);
        record.setHandledAt(LocalDateTime.now());
        if (result != null && !result.isEmpty()) {
            record.setAlertContent(record.getAlertContent() + " [处理结果: " + result + "]");
        }
        alertRecordRepository.updateById(record);
    }

    // ========== 辅助方法 ==========

    private MaterialAlertRecordDO buildAlertRecord(Long materialId, Long batchId, int alertType) {
        MaterialAlertRecordDO record = new MaterialAlertRecordDO();
        record.setMaterialId(materialId);
        record.setBatchId(batchId);
        record.setAlertType(alertType);
        record.setStatus(0);
        record.setAlertTime(LocalDateTime.now());
        return record;
    }

    /** 推送预警消息（目标：所有管理员） */
    private void sendWarningMessage(String messageType, String materialName,
                                     String batchNo, LocalDate expiryDate, Integer quantity) {
        MessagePublishCommand command = MessagePublishCommand.builder()
                .sourceSystem("material")
                .messageType(messageType)
                .templateCode(messageType)
                .targetType(TargetType.ALL_MANAGER)
                .params(Map.of(
                        "materialName", materialName != null ? materialName : "",
                        "batchNo", batchNo != null ? batchNo : "",
                        "expiryDate", expiryDate != null ? expiryDate.toString() : "",
                        "quantity", quantity != null ? String.valueOf(quantity) : ""
                ))
                .occurredAt(LocalDateTime.now())
                .build();
        messagePublisher.publish(command);
    }

    private MaterialAlertVO toAlertVO(MaterialAlertRecordDO record) {
        MaterialDO material = materialRepository.findById(record.getMaterialId()).orElse(null);
        MaterialBatchDO batch = record.getBatchId() != null
                ? batchRepository.findById(record.getBatchId()).orElse(null) : null;
        return MaterialAlertVO.builder()
                .id(record.getId())
                .materialId(record.getMaterialId())
                .materialName(material != null ? material.getMaterialName() : null)
                .batchId(record.getBatchId())
                .batchNo(batch != null ? batch.getBatchNo() : null)
                .alertType(record.getAlertType())
                .alertTypeDesc(AlertType.fromCode(record.getAlertType()).getDesc())
                .alertContent(record.getAlertContent())
                .status(record.getStatus())
                .statusDesc(AlertStatus.fromCode(record.getStatus()).getDesc())
                .handledBy(record.getHandledBy())
                .alertTime(record.getAlertTime())
                .handledAt(record.getHandledAt())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
