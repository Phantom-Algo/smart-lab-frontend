package com.zhangjintao.project.smart_lab_backend.modules.material.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.material.config.MaterialConfig;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.enums.ApplyStatus;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.material.exception.MaterialException;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveLogQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveDetailVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveLogVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialReceiveService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 领用管理服务实现 — FR-0003/FR-0004/FR-0005
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialReceiveServiceImpl implements MaterialReceiveService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialReceiveApplyRepository applyRepository;
    private final MaterialReceiveLogRepository logRepository;
    private final MaterialReceiveBatchDetailRepository detailRepository;
    private final MaterialAlertService materialAlertService;
    private final MessagePublisher messagePublisher;
    private final PersonDirectoryPort personDirectoryPort;
    private final MaterialConfig config;
    private final RealtimePublisher realtimePublisher;

    /** 当日自增序列（避免同毫秒编号碰撞） */
    private final AtomicInteger dailySeq = new AtomicInteger(1);
    private volatile String dailySeqDate = "";

    // ==================== FR-0003 领用申请与取消 ====================

    @Override
    @Transactional
    public ReceiveApplyVO apply(ReceiveApplyDTO dto, Long applicantId) {
        // 1. 校验物资存在
        MaterialDO material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        // 2. 校验库存充足
        if (material.getCurrentStock() < dto.getExpectedQuantity()) {
            throw new MaterialException(MaterialErrorCode.STOCK_INSUFFICIENT,
                    "当前库存" + material.getCurrentStock() + "，申请" + dto.getExpectedQuantity());
        }
        // 3. 校验无重复待审核申请（同一物资+同一申请人）
        if (applyRepository.existsPendingApply(dto.getMaterialId(), applicantId)) {
            throw new MaterialException(MaterialErrorCode.APPLY_DUPLICATE_PENDING);
        }
        // 4. 创建申请单
        MaterialReceiveApplyDO applyDO = new MaterialReceiveApplyDO();
        applyDO.setApplyNo(generateApplyNo());
        applyDO.setMaterialId(dto.getMaterialId());
        applyDO.setApplicantId(applicantId);
        applyDO.setExpectedQuantity(dto.getExpectedQuantity());
        applyDO.setPurpose(dto.getPurpose());
        applyDO.setApplyStatus(1); // 待审核
        applyRepository.insert(applyDO);
	        publishRealtime("material.receive_submitted", material, Map.of(
	            "applyNo", applyDO.getApplyNo(),
	            "applicantId", applicantId,
	            "expectedQuantity", applyDO.getExpectedQuantity(),
	            "applyStatus", applyDO.getApplyStatus(),
	            "applyStatusDesc", ApplyStatus.fromCode(applyDO.getApplyStatus()).getDesc()));
	        publishPendingReviewMessage(applyDO, material);
	        log.info("领用申请创建成功，applyNo={}, materialId={}, quantity={}",
	                applyDO.getApplyNo(), dto.getMaterialId(), dto.getExpectedQuantity());
	        return toApplyVO(applyDO);
    }

    @Override
    @Transactional
    public void cancel(String applyNo, Long applicantId) {
        MaterialReceiveApplyDO applyDO = applyRepository.findByApplyNo(applyNo)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.APPLY_NOT_FOUND));
        // 仅允许申请人取消自己的申请
        if (!applyDO.getApplicantId().equals(applicantId)) {
            throw new MaterialException(MaterialErrorCode.APPLY_STATUS_INVALID, "仅申请人可取消");
        }
        // 仅允许取消待审核的申请
        if (applyDO.getApplyStatus() != 1) {
            throw new MaterialException(MaterialErrorCode.APPLY_STATUS_INVALID);
        }
        applyDO.setApplyStatus(4); // 已取消
        applyRepository.updateById(applyDO);
        MaterialDO material = materialRepository.findById(applyDO.getMaterialId()).orElse(null);
        if (material != null) {
            publishRealtime("material.receive_cancelled", material, Map.of(
                    "applyNo", applyNo,
                    "applicantId", applicantId,
                    "applyStatus", applyDO.getApplyStatus(),
                    "applyStatusDesc", ApplyStatus.fromCode(applyDO.getApplyStatus()).getDesc()));
        }
        log.info("领用申请已取消，applyNo={}", applyNo);
    }

    // ==================== FR-0004 审批与出库 ====================

    @Override
    @Transactional
    public ReceiveLogVO approve(String applyNo, Long approverId) {
        // 1. 校验申请单
        MaterialReceiveApplyDO applyDO = applyRepository.findByApplyNo(applyNo)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.APPLY_NOT_FOUND));
        if (applyDO.getApplyStatus() != 1) {
            throw new MaterialException(MaterialErrorCode.APPLY_STATUS_INVALID);
        }
        // 2. 再次校验库存（防止并发）
        MaterialDO material = materialRepository.findById(applyDO.getMaterialId())
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.MATERIAL_NOT_FOUND));
        if (material.getCurrentStock() < applyDO.getExpectedQuantity()) {
            throw new MaterialException(MaterialErrorCode.STOCK_INSUFFICIENT,
                    "当前库存" + material.getCurrentStock() + "，审批数量" + applyDO.getExpectedQuantity());
        }
        // 3. FIFO + 临期优先选批算法（expiryDate ASC, stockInTime ASC）
        List<MaterialBatchDO> availableBatches = batchRepository.findAvailableByMaterialId(applyDO.getMaterialId());
        int toDeduct = applyDO.getExpectedQuantity();
        List<MaterialReceiveBatchDetailDO> details = new ArrayList<>();
        for (MaterialBatchDO batch : availableBatches) {
            if (toDeduct <= 0) break;
            int deduct = Math.min(toDeduct, batch.getRemaining());
            int affected = batchRepository.deductRemaining(batch.getId(), deduct);
            if (affected == 0) {
                throw new MaterialException(MaterialErrorCode.STOCK_INSUFFICIENT,
                        "批次并发扣减冲突，batchId=" + batch.getId());
            }
            // 扣减后余量为0 → 状态改为已清空
            if (deduct == batch.getRemaining()) {
                batchRepository.updateBatchStatus(batch.getId(), 2);
            }
            MaterialReceiveBatchDetailDO detail = new MaterialReceiveBatchDetailDO();
            detail.setBatchId(batch.getId());
            detail.setDeductedQuantity(deduct);
            details.add(detail);
            toDeduct -= deduct;
        }
        if (toDeduct > 0) {
            throw new MaterialException(MaterialErrorCode.STOCK_INSUFFICIENT,
                    "批次扣减后仍有" + toDeduct + "未满足");
        }
        // 4. 更新物资总库存
        materialRepository.updateCurrentStock(material.getId(), -applyDO.getExpectedQuantity());
        // 5. 写入出库日志
        MaterialReceiveLogDO logDO = new MaterialReceiveLogDO();
        logDO.setApplyId(applyDO.getId());
        logDO.setMaterialId(applyDO.getMaterialId());
        logDO.setTotalQuantity(applyDO.getExpectedQuantity());
        logDO.setReceiverId(applyDO.getApplicantId());
        logDO.setOutOperatorId(approverId);
        logDO.setOutTime(LocalDateTime.now());
        logRepository.insert(logDO);
        // 6. 写入批次扣减明细
        for (MaterialReceiveBatchDetailDO detail : details) {
            detail.setReceiveLogId(logDO.getId());
        }
        detailRepository.insertBatch(details);
        // 7. 更新申请状态为审批通过
        applyDO.setApplyStatus(2);
        applyDO.setApproverId(approverId);
        applyRepository.updateById(applyDO);
        publishRealtime("material.receive_approved", material, Map.of(
            "applyNo", applyNo,
            "applicantId", applyDO.getApplicantId(),
            "approverId", approverId,
            "quantity", applyDO.getExpectedQuantity(),
            "applyStatus", applyDO.getApplyStatus(),
            "applyStatusDesc", ApplyStatus.fromCode(applyDO.getApplyStatus()).getDesc()));
        MaterialDO updatedMaterial = materialRepository.findById(material.getId()).orElse(material);
        publishRealtime("material.stock_changed", updatedMaterial, Map.of(
            "applyNo", applyNo,
            "delta", -applyDO.getExpectedQuantity(),
            "reason", "receive_approved"));
        // 超量领用预警检测（单次领用超过阈值时触发）
        materialAlertService.createOverReceiveAlert(applyDO.getMaterialId(), applyDO.getExpectedQuantity());
        // 推送审批通过通知至申请人
	        sendMessage("material.normal.receive.approved", applyDO.getApplicantId(),
	                "耗材领用申请已通过",
	                "你的耗材领用申请已审核通过。"
	                        + "\n耗材：" + material.getMaterialName()
	                        + "\n数量：" + applyDO.getExpectedQuantity()
	                        + "\n申请单号：" + applyDO.getApplyNo());
        log.info("领用审批通过并出库，applyNo={}, totalQuantity={}", applyNo, applyDO.getExpectedQuantity());
        return toLogVO(logDO);
    }

    @Override
    @Transactional
    public void reject(String applyNo, Long approverId, String reason) {
        MaterialReceiveApplyDO applyDO = applyRepository.findByApplyNo(applyNo)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.APPLY_NOT_FOUND));
        if (applyDO.getApplyStatus() != 1) {
            throw new MaterialException(MaterialErrorCode.APPLY_STATUS_INVALID);
        }
        applyDO.setApplyStatus(3); // 已驳回
        applyDO.setApproverId(approverId);
        applyDO.setRejectReason(reason);
        applyRepository.updateById(applyDO);
        // 推送驳回通知至申请人
        String materialName = materialRepository.findById(applyDO.getMaterialId())
                .map(MaterialDO::getMaterialName).orElse("");
        materialRepository.findById(applyDO.getMaterialId()).ifPresent(material ->
            publishRealtime("material.receive_rejected", material, Map.of(
                "applyNo", applyNo,
                "applicantId", applyDO.getApplicantId(),
                "approverId", approverId,
                "quantity", applyDO.getExpectedQuantity(),
                "applyStatus", applyDO.getApplyStatus(),
                "applyStatusDesc", ApplyStatus.fromCode(applyDO.getApplyStatus()).getDesc(),
                "rejectReason", reason)));
	        sendMessage("material.normal.receive.rejected", applyDO.getApplicantId(),
	                "耗材领用申请已驳回",
	                "你的耗材领用申请已被驳回。"
	                        + "\n耗材：" + materialName
	                        + "\n数量：" + applyDO.getExpectedQuantity()
	                        + "\n申请单号：" + applyDO.getApplyNo()
	                        + "\n驳回原因：" + reason);
        log.info("领用申请已驳回，applyNo={}, reason={}", applyNo, reason);
    }

    // ==================== FR-0005 领用记录查询 ====================

    @Override
    public PageResult<ReceiveApplyVO> pageApplyRecords(ReceiveLogQueryDTO query) {
        Page<MaterialReceiveApplyDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // receiverId 作为申请人过滤（领用记录按领用人查询）
        IPage<MaterialReceiveApplyDO> result = applyRepository.pageByCondition(
                query.getMaterialId(), query.getReceiverId(), query.getApplyStatus(),
                query.getStartTime(), query.getEndTime(), page);
        List<ReceiveApplyVO> vos = result.getRecords().stream()
                .map(this::toApplyVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ReceiveDetailVO getReceiveDetail(String applyNo) {
        MaterialReceiveApplyDO apply = applyRepository.findByApplyNo(applyNo)
                .orElseThrow(() -> new MaterialException(MaterialErrorCode.APPLY_NOT_FOUND));
        ReceiveApplyVO applyVO = toApplyVO(apply);
        // 审批通过且有出库日志时，组装出库信息
        ReceiveLogVO logVO = null;
        if (apply.getApplyStatus() == 2) {
            MaterialReceiveLogDO logDO = logRepository.findByApplyId(apply.getId()).orElse(null);
            if (logDO != null) {
                logVO = toLogVO(logDO);
            }
        }
        return ReceiveDetailVO.builder()
                .apply(applyVO)
                .receiveLog(logVO)
                .build();
    }

    // ========== 辅助方法 ==========

    private String generateApplyNo() {
        return generateDailySeqNo(config.getApplyNoPrefix());
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

	    private void publishPendingReviewMessage(MaterialReceiveApplyDO applyDO, MaterialDO material) {
	        String applicantName = personDirectoryPort.getByPersonId(applyDO.getApplicantId())
	                .map(PersonDirectoryItemDTO::getPersonName)
	                .orElse("未知用户");
	        String title = "新的耗材领用申请待审批";
	        String content = "有新的耗材领用申请需要处理。"
	                + "\n申请人：" + applicantName
	                + "\n耗材：" + material.getMaterialName()
	                + "\n数量：" + applyDO.getExpectedQuantity()
	                + "\n申请单号：" + applyDO.getApplyNo()
	                + "\n用途：" + applyDO.getPurpose();
	        try {
	            messagePublisher.publish(MessagePublishCommand.builder()
	                    .eventNo(MessageEventNoGenerator.generate("material.system.receive.pending_review"))
	                    .sourceSystem("material")
	                    .messageType("material.system.receive.pending_review")
	                    .targetType(TargetType.ALL_MANAGER)
	                    .tempTemplate(TempMessageTemplate.builder()
	                            .titleTemplate(title)
	                            .contentTemplate(content)
	                            .contentFormat(ContentFormat.TEXT)
	                            .build())
	                    .occurredAt(LocalDateTime.now())
	                    .build());
	        } catch (Exception ex) {
	            log.warn("耗材领用待审批消息发送失败，applyNo={}", applyDO.getApplyNo(), ex);
	        }
	    }

	    /** 推送消息：personId → userId 解析 + 发送 */
	    private void sendMessage(String messageType, Long targetPersonId, String title, String content) {
	        Long userId = resolveUserId(targetPersonId);
	        if (userId == null) {
	            log.warn("人员无对应用户账户，消息跳过，personId={}, messageType={}", targetPersonId, messageType);
	            return;
	        }
	        try {
	            MessagePublishCommand command = MessagePublishCommand.builder()
	                    .eventNo(MessageEventNoGenerator.generate(messageType))
	                    .sourceSystem("material")
	                    .messageType(messageType)
	                    .targetType(TargetType.SINGLE)
	                    .targetUserIds(List.of(userId))
	                    .tempTemplate(TempMessageTemplate.builder()
	                            .titleTemplate(title)
	                            .contentTemplate(content)
	                            .contentFormat(ContentFormat.TEXT)
	                            .build())
	                    .occurredAt(LocalDateTime.now())
	                    .build();
	            messagePublisher.publish(command);
	        } catch (Exception ex) {
	            log.warn("耗材消息发送失败，messageType={}, targetUserId={}", messageType, userId, ex);
	        }
	    }

    /** personId → userId 映射（发消息用） */
    private Long resolveUserId(Long personId) {
        return personDirectoryPort.getByPersonId(personId)
                .map(PersonDirectoryItemDTO::getUserId)
                .orElse(null);
    }

    private ReceiveApplyVO toApplyVO(MaterialReceiveApplyDO apply) {
        MaterialDO material = materialRepository.findById(apply.getMaterialId()).orElse(null);
        return ReceiveApplyVO.builder()
                .id(apply.getId())
                .applyNo(apply.getApplyNo())
                .materialId(apply.getMaterialId())
                .materialName(material != null ? material.getMaterialName() : null)
                .applicantId(apply.getApplicantId())
                .approverId(apply.getApproverId())
                .expectedQuantity(apply.getExpectedQuantity())
                .purpose(apply.getPurpose())
                .applyStatus(apply.getApplyStatus())
                .applyStatusDesc(ApplyStatus.fromCode(apply.getApplyStatus()).getDesc())
                .rejectReason(apply.getRejectReason())
                .createdAt(apply.getCreatedAt())
                .updatedAt(apply.getUpdatedAt())
                .build();
    }

    private ReceiveLogVO toLogVO(MaterialReceiveLogDO logDO) {
        MaterialDO material = materialRepository.findById(logDO.getMaterialId()).orElse(null);
        String applyNo = applyRepository.findById(logDO.getApplyId())
                .map(MaterialReceiveApplyDO::getApplyNo).orElse(null);
        List<MaterialReceiveBatchDetailDO> details = detailRepository.findByReceiveLogId(logDO.getId());
        return ReceiveLogVO.builder()
                .id(logDO.getId())
                .applyId(logDO.getApplyId())
                .applyNo(applyNo)
                .materialId(logDO.getMaterialId())
                .materialName(material != null ? material.getMaterialName() : null)
                .totalQuantity(logDO.getTotalQuantity())
                .receiverId(logDO.getReceiverId())
                .outOperatorId(logDO.getOutOperatorId())
                .outTime(logDO.getOutTime())
                .remark(logDO.getRemark())
                .batchDetails(details.stream().map(d -> {
                    MaterialBatchDO b = batchRepository.findById(d.getBatchId()).orElse(null);
                    return ReceiveLogVO.BatchDeductDetail.builder()
                        .batchId(d.getBatchId())
                        .batchNo(b != null ? b.getBatchNo() : null)
                        .deductedQuantity(d.getDeductedQuantity())
                        .build();
                }).collect(Collectors.toList()))
                .createdAt(logDO.getCreatedAt())
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
