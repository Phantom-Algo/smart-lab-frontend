package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowApplyDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceBorrowApplyMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowApplyRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.ApplyStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.BorrowStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.DeviceStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowApproveDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowLogQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowLogVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceBorrowService;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备借用管理服务实现
 * <p>
 * 所有人员ID均通过 {@link PersonDirectoryPort} 与人员管理系统对接校验，
 * 发送消息时通过 personId → userId 映射确保消息送达正确用户。
 * </p>
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceBorrowServiceImpl implements DeviceBorrowService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DeviceBorrowApplyRepository applyRepository;
    private final DeviceBorrowLogRepository borrowLogRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceBorrowApplyMapper applyMapper;
    private final EquipmentConfig config;
    private final MessagePublisher messagePublisher;
    private final PersonDirectoryPort personDirectoryPort;
    private final RealtimePublisher realtimePublisher;

    // ========== FR-0004: 借用申请 ==========

    @Override
    @Transactional
    public BorrowApplyVO apply(Long deviceId, BorrowApplyDTO dto) {
        // 0. 校验申请人在人员管理系统中存在
        PersonDirectoryItemDTO applicant = personDirectoryPort.getByPersonId(dto.getApplicantId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "申请人不存在"));

        // 1. 校验设备存在且状态为闲置
        DeviceDO device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
        if (device.getDeviceStatus() != DeviceStatus.IDLE.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_NOT_AVAILABLE);
        }

        // 2. 校验同一设备+同一申请人无待审核申请
        if (applyRepository.existsPendingApply(deviceId, dto.getApplicantId())) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_DEVICE_CONFLICT);
        }

        // 3. 生成申请单号并创建申请
        DeviceBorrowApplyDO apply = new DeviceBorrowApplyDO();
        apply.setApplyNo(generateApplyNo());
        apply.setDeviceId(deviceId);
        apply.setApplicantId(dto.getApplicantId());
        apply.setExpectedStartTime(dto.getExpectedStartTime());
        apply.setExpectedReturnTime(dto.getExpectedReturnTime());
        apply.setPurpose(dto.getPurpose());
        apply.setApplyStatus(ApplyStatus.PENDING.getCode());
        applyRepository.insert(apply);
        publishRealtime("equipment.borrow_submitted", Map.of(
            "applyNo", apply.getApplyNo(),
            "deviceId", deviceId,
            "applicantId", dto.getApplicantId(),
            "applyStatus", apply.getApplyStatus(),
            "applyStatusDesc", ApplyStatus.PENDING.getDesc(),
            "expectedStartTime", dto.getExpectedStartTime(),
            "expectedReturnTime", dto.getExpectedReturnTime()));
        publishBorrowPendingReviewMessage(apply, device, applicant);

        log.info("借用申请提交成功，applyNo={}, deviceId={}, applicantId={}",
                apply.getApplyNo(), deviceId, dto.getApplicantId());
        return toApplyVO(apply, applicant.getPersonName());
    }

    @Override
    @Transactional
    public void cancelApply(String applyNo) {
        DeviceBorrowApplyDO apply = findByApplyNo(applyNo);
        if (apply == null) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_NOT_FOUND);
        }
        if (apply.getApplyStatus() != ApplyStatus.PENDING.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_STATUS_INVALID);
        }
        apply.setApplyStatus(ApplyStatus.CANCELLED.getCode());
        applyRepository.updateById(apply);
        publishRealtime("equipment.borrow_cancelled", Map.of(
            "applyNo", applyNo,
            "deviceId", apply.getDeviceId(),
            "applicantId", apply.getApplicantId(),
            "applyStatus", apply.getApplyStatus(),
            "applyStatusDesc", ApplyStatus.CANCELLED.getDesc()));
        log.info("借用申请已取消，applyNo={}", applyNo);
    }

    // ========== FR-0005: 借用审批 ==========

    @Override
    @Transactional
    public void approve(String applyNo, BorrowApproveDTO dto) {
        DeviceBorrowApplyDO apply = findByApplyNo(applyNo);
        if (apply == null) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_NOT_FOUND);
        }
        if (apply.getApplyStatus() != ApplyStatus.PENDING.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_STATUS_INVALID);
        }

        // 校验审批人在人员管理系统中存在
        personDirectoryPort.getByPersonId(dto.getApproverId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "审批人不存在"));
        DeviceDO device = deviceRepository.findById(apply.getDeviceId()).orElse(null);
        String deviceName = device != null ? device.getName() : "未知设备";

        if (Boolean.TRUE.equals(dto.getApproved())) {
            applyRepository.updateStatus(applyNo, ApplyStatus.APPROVED.getCode(),
                    dto.getApproverId(), null);
            publishRealtime("equipment.borrow_approved", Map.of(
                "applyNo", applyNo,
                "deviceId", apply.getDeviceId(),
                "applicantId", apply.getApplicantId(),
                "approverId", dto.getApproverId(),
                "applyStatus", ApplyStatus.APPROVED.getCode(),
                "applyStatusDesc", ApplyStatus.APPROVED.getDesc()));
            log.info("借用申请审批通过，applyNo={}, approverId={}", applyNo, dto.getApproverId());

            // 通知申请人审批通过
            Long applicantUserId = resolveUserId(apply.getApplicantId());
            if (applicantUserId != null) {
                publishBorrowMessage("equipment.normal.borrow.approved", applicantUserId,
                        "设备借用申请已通过",
                        "你的设备借用申请已审核通过。"
                                + "\n设备：" + deviceName
                                + "\n申请单号：" + applyNo
                                + "\n预计借用：" + apply.getExpectedStartTime()
                                + "\n预计归还：" + apply.getExpectedReturnTime());
            }
        } else {
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "驳回理由不能为空");
            }
            applyRepository.updateStatus(applyNo, ApplyStatus.REJECTED.getCode(),
                    dto.getApproverId(), dto.getRejectReason());
            publishRealtime("equipment.borrow_rejected", Map.of(
                "applyNo", applyNo,
                "deviceId", apply.getDeviceId(),
                "applicantId", apply.getApplicantId(),
                "approverId", dto.getApproverId(),
                "applyStatus", ApplyStatus.REJECTED.getCode(),
                "applyStatusDesc", ApplyStatus.REJECTED.getDesc(),
                "rejectReason", dto.getRejectReason()));
            log.info("借用申请已驳回，applyNo={}, reason={}", applyNo, dto.getRejectReason());

            // 通知申请人审批驳回
            Long applicantUserId = resolveUserId(apply.getApplicantId());
            if (applicantUserId != null) {
                publishBorrowMessage("equipment.normal.borrow.rejected", applicantUserId,
                        "设备借用申请已驳回",
                        "你的设备借用申请已被驳回。"
                                + "\n设备：" + deviceName
                                + "\n申请单号：" + applyNo
                                + "\n驳回原因：" + dto.getRejectReason());
            }
        }
    }

    // ========== FR-0006: 领用登记 ==========

    @Override
    @Transactional
    public BorrowLogVO checkOut(String applyNo, Long outOperatorId) {
        DeviceBorrowApplyDO apply = findByApplyNo(applyNo);
        if (apply == null) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_NOT_FOUND);
        }
        if (apply.getApplyStatus() != ApplyStatus.APPROVED.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.APPLY_STATUS_INVALID);
        }

        // 校验操作人在人员管理系统中存在
        personDirectoryPort.getByPersonId(outOperatorId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "领用操作人不存在"));

        DeviceDO device = deviceRepository.findById(apply.getDeviceId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
        if (device.getDeviceStatus() != DeviceStatus.IDLE.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_NOT_AVAILABLE);
        }

        // 创建借用记录
        DeviceBorrowLogDO logDO = new DeviceBorrowLogDO();
        logDO.setApplyId(apply.getId());
        logDO.setDeviceId(apply.getDeviceId());
        logDO.setBorrowerId(apply.getApplicantId());
        logDO.setOutOperatorId(outOperatorId);
        logDO.setExpectedReturnTime(apply.getExpectedReturnTime());
        logDO.setActualOutTime(LocalDateTime.now());
        logDO.setStatus(BorrowStatus.BORROWING.getCode());
        borrowLogRepository.insert(logDO);

        // 更新设备状态为借用中
        deviceRepository.updateStatusWithVersion(apply.getDeviceId(),
                DeviceStatus.BORROWED.getCode(), device.getVersion());
        publishRealtime("equipment.borrow_checked_out", Map.of(
            "applyNo", applyNo,
            "borrowLogId", logDO.getId(),
            "deviceId", apply.getDeviceId(),
            "borrowerId", apply.getApplicantId(),
            "outOperatorId", outOperatorId,
            "status", BorrowStatus.BORROWING.getCode(),
            "statusDesc", BorrowStatus.BORROWING.getDesc()));
        publishDeviceStatusChanged(apply.getDeviceId(), device.getName(),
            DeviceStatus.IDLE, DeviceStatus.BORROWED, "设备领用登记");

        log.info("设备领用登记成功，applyNo={}, deviceId={}, borrowerId={}, outOperatorId={}",
                applyNo, apply.getDeviceId(), apply.getApplicantId(), outOperatorId);

        // 归还提醒
        Long borrowerUserId = resolveUserId(apply.getApplicantId());
        if (borrowerUserId != null) {
            publishBorrowMessage("equipment.normal.return.reminder", borrowerUserId,
                    "设备归还提醒",
                    "你借用的设备请按时归还。"
                            + "\n设备：" + device.getName()
                            + "\n预计归还时间：" + apply.getExpectedReturnTime());
        }
        return toLogVO(logDO);
    }

    // ========== FR-0007: 归还登记 ==========

    @Override
    @Transactional
    public void checkIn(Long borrowLogId, String returnRemark, Long returnOperatorId) {
        DeviceBorrowLogDO logDO = borrowLogRepository.findById(borrowLogId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.BORROW_LOG_NOT_FOUND));
        if (logDO.getStatus() != BorrowStatus.BORROWING.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.BORROW_STATUS_INVALID);
        }

        // 校验操作人在人员管理系统中存在
        personDirectoryPort.getByPersonId(returnOperatorId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "归还操作人不存在"));

        DeviceDO device = deviceRepository.findById(logDO.getDeviceId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));

        // 判断是否逾期
        boolean isOverdue = logDO.getExpectedReturnTime() != null
                && LocalDateTime.now().isAfter(logDO.getExpectedReturnTime());

        logDO.setStatus(BorrowStatus.RETURNED.getCode());
        logDO.setActualReturnTime(LocalDateTime.now());
        logDO.setIsOverdue(isOverdue ? 1 : 0);
        logDO.setReturnRemark(returnRemark);
        logDO.setReturnOperatorId(returnOperatorId);
        borrowLogRepository.updateById(logDO);

        // 恢复设备为闲置
        deviceRepository.updateStatusWithVersion(logDO.getDeviceId(),
                DeviceStatus.IDLE.getCode(), device.getVersion());
        publishRealtime("equipment.borrow_returned", Map.of(
            "borrowLogId", borrowLogId,
            "deviceId", logDO.getDeviceId(),
            "borrowerId", logDO.getBorrowerId(),
            "returnOperatorId", returnOperatorId,
            "status", BorrowStatus.RETURNED.getCode(),
            "statusDesc", BorrowStatus.RETURNED.getDesc(),
            "isOverdue", isOverdue,
            "returnRemark", returnRemark));
        publishDeviceStatusChanged(logDO.getDeviceId(), device.getName(),
            DeviceStatus.BORROWED, DeviceStatus.IDLE, "设备归还登记");

        log.info("设备归还成功，borrowLogId={}, deviceName={}, isOverdue={}",
                borrowLogId, device.getName(), isOverdue);
    }

    // ========== FR-0008: 借用记录查询 ==========

    @Override
    public PageResult<BorrowLogVO> listBorrowLogs(BorrowLogQueryDTO query) {
        IPage<DeviceBorrowLogDO> page = borrowLogRepository.pageByCondition(
                Page.of(query.getPageNum(), query.getPageSize()),
                query.getDeviceId(), query.getBorrowerId(), query.getStatus());
        return PageResult.of(
                page.getRecords().stream().map(this::toLogVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }

    @Override
    public PageResult<BorrowApplyVO> listMyApplies(Long applicantId, Integer applyStatus, long pageNum, long pageSize) {
        return listApplies(null, null, applicantId, applyStatus, pageNum, pageSize);
    }

    @Override
    public PageResult<BorrowApplyVO> listApplies(String applyNo, Long deviceId, Long applicantId,
            Integer applyStatus, long pageNum, long pageSize) {
        IPage<DeviceBorrowApplyDO> page = applyRepository.pageByCondition(
                Page.of(pageNum, pageSize),
                applyNo, deviceId, applicantId, applyStatus);
        return PageResult.of(
                page.getRecords().stream().map(this::toApplyVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }

    // ========== FR-0009: 逾期检测 ==========

    @Override
    @Transactional
    public void detectOverdue() {
        List<DeviceBorrowLogDO> overdueLogs = borrowLogRepository.findOverdueLogs();
        if (overdueLogs.isEmpty()) {
            log.debug("无逾期借用记录");
            return;
        }
        for (DeviceBorrowLogDO logDO : overdueLogs) {
            logDO.setStatus(BorrowStatus.OVERDUE_UNRETURNED.getCode());
            logDO.setIsOverdue(1);
            borrowLogRepository.updateById(logDO);

            DeviceDO device = deviceRepository.findById(logDO.getDeviceId()).orElse(null);
            String deviceName = device != null ? device.getName() : "未知设备";

            // 将 borrowerId(person_id) 转换为 userId，确保消息正确送达
            Long borrowerUserId = resolveUserId(logDO.getBorrowerId());
            if (borrowerUserId != null) {
                publishBorrowMessage("equipment.warning.borrow.overdue", borrowerUserId,
                        "设备借用已逾期",
                        "你借用的设备已超过预计归还时间，请尽快归还。"
                                + "\n设备：" + deviceName
                                + "\n预计归还时间：" + logDO.getExpectedReturnTime());
            }
            publishRealtime("equipment.borrow_overdue", Map.of(
                    "borrowLogId", logDO.getId(),
                    "deviceId", logDO.getDeviceId(),
                    "borrowerId", logDO.getBorrowerId(),
                    "expectedReturnTime", logDO.getExpectedReturnTime(),
                    "status", BorrowStatus.OVERDUE_UNRETURNED.getCode(),
                    "statusDesc", BorrowStatus.OVERDUE_UNRETURNED.getDesc()));
        }
        log.info("逾期检测完成，标记逾期记录数={}", overdueLogs.size());
    }

    // ========== 私有方法 ==========

    /**
     * 通过 personId 查找对应的 userId，用于消息通知
     */
    private Long resolveUserId(Long personId) {
        if (personId == null) return null;
        return personDirectoryPort.getByPersonId(personId)
                .map(PersonDirectoryItemDTO::getUserId)
                .orElse(null);
    }

    private String generateApplyNo() {
        String prefix = config.getApplyNoPrefix() + LocalDate.now().format(DATE_FORMAT);
        String maxNo = getMaxApplyNoByPrefix(prefix);
        return prefix + nextSequence(maxNo, prefix);
    }

    private String getMaxApplyNoByPrefix(String prefix) {
        List<DeviceBorrowApplyDO> list = applyMapper.selectList(
                new LambdaQueryWrapper<DeviceBorrowApplyDO>()
                        .likeRight(DeviceBorrowApplyDO::getApplyNo, prefix)
                        .orderByDesc(DeviceBorrowApplyDO::getApplyNo)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getApplyNo();
    }

    private String nextSequence(String maxNo, String prefix) {
        if (maxNo == null) return "000001";
        String seq = maxNo.substring(prefix.length());
        return String.format("%06d", Integer.parseInt(seq) + 1);
    }

    private DeviceBorrowApplyDO findByApplyNo(String applyNo) {
        List<DeviceBorrowApplyDO> list = applyMapper.selectList(
                new LambdaQueryWrapper<DeviceBorrowApplyDO>()
                        .eq(DeviceBorrowApplyDO::getApplyNo, applyNo));
        return list.isEmpty() ? null : list.get(0);
    }

    private BorrowApplyVO toApplyVO(DeviceBorrowApplyDO apply, String applicantName) {
        DeviceDO device = deviceRepository.findById(apply.getDeviceId()).orElse(null);
        PersonDirectoryItemDTO applicant = personDirectoryPort.getByPersonId(apply.getApplicantId()).orElse(null);
        ApplyStatus statusEnum = ApplyStatus.fromCode(apply.getApplyStatus());
        return BorrowApplyVO.builder()
                .id(apply.getId())
                .applyNo(apply.getApplyNo())
                .deviceId(apply.getDeviceId())
                .deviceNo(device != null ? device.getDeviceNo() : null)
                .deviceName(device != null ? device.getName() : null)
                .applicantId(apply.getApplicantId())
                .applicantName(applicantName != null ? applicantName : applicant != null ? applicant.getPersonName() : null)
                .applicantType(applicant != null ? applicant.getPersonType() : null)
                .approverId(apply.getApproverId())
                .expectedStartTime(apply.getExpectedStartTime())
                .expectedReturnTime(apply.getExpectedReturnTime())
                .purpose(apply.getPurpose())
                .applyStatus(apply.getApplyStatus())
                .applyStatusDesc(statusEnum != null ? statusEnum.getDesc() : null)
                .rejectReason(apply.getRejectReason())
                .isOverdue(apply.getIsOverdue())
                .createdAt(apply.getCreatedAt())
                .updatedAt(apply.getUpdatedAt())
                .build();
    }

    private BorrowApplyVO toApplyVO(DeviceBorrowApplyDO apply) {
        return toApplyVO(apply, null);
    }

    private BorrowLogVO toLogVO(DeviceBorrowLogDO logDO) {
        DeviceDO device = deviceRepository.findById(logDO.getDeviceId()).orElse(null);
        BorrowStatus statusEnum = BorrowStatus.fromCode(logDO.getStatus());
        return BorrowLogVO.builder()
                .id(logDO.getId())
                .applyId(logDO.getApplyId())
                .deviceId(logDO.getDeviceId())
                .deviceName(device != null ? device.getName() : null)
                .borrowerId(logDO.getBorrowerId())
                .outOperatorId(logDO.getOutOperatorId())
                .returnOperatorId(logDO.getReturnOperatorId())
                .expectedReturnTime(logDO.getExpectedReturnTime())
                .actualOutTime(logDO.getActualOutTime())
                .actualReturnTime(logDO.getActualReturnTime())
                .status(logDO.getStatus())
                .statusDesc(statusEnum != null ? statusEnum.getDesc() : null)
                .isOverdue(logDO.getIsOverdue())
                .returnRemark(logDO.getReturnRemark())
                .createdAt(logDO.getCreatedAt())
                .updatedAt(logDO.getUpdatedAt())
                .build();
    }

    private void publishBorrowPendingReviewMessage(DeviceBorrowApplyDO apply, DeviceDO device,
            PersonDirectoryItemDTO applicant) {
        String title = "新的设备借用申请待审批";
        String content = "有新的设备借用申请需要处理。"
                + "\n申请人：" + applicant.getPersonName()
                + "\n设备：" + device.getName()
                + "\n申请单号：" + apply.getApplyNo()
                + "\n预计借用：" + apply.getExpectedStartTime()
                + "\n预计归还：" + apply.getExpectedReturnTime()
                + "\n用途：" + apply.getPurpose();
        try {
            MessagePublishCommand command = MessagePublishCommand.builder()
                    .eventNo(MessageEventNoGenerator.generate("equipment.system.borrow.pending_review"))
                    .sourceSystem("equipment")
                    .messageType("equipment.system.borrow.pending_review")
                    .targetType(TargetType.ALL_MANAGER)
                    .tempTemplate(TempMessageTemplate.builder()
                            .titleTemplate(title)
                            .contentTemplate(content)
                            .contentFormat(ContentFormat.TEXT)
                            .build())
                    .occurredAt(LocalDateTime.now())
                    .build();
            messagePublisher.publish(command);
            log.info("设备借用待审批消息已发送，applyNo={}", apply.getApplyNo());
        } catch (Exception e) {
            log.error("设备借用待审批消息发送失败，applyNo={}", apply.getApplyNo(), e);
        }
    }

    private void publishBorrowMessage(String messageType, Long targetUserId, String title, String content) {
        try {
            MessagePublishCommand command = MessagePublishCommand.builder()
                    .eventNo(MessageEventNoGenerator.generate(messageType))
                    .sourceSystem("equipment")
                    .messageType(messageType)
                    .targetType(TargetType.SINGLE)
                    .targetUserIds(List.of(targetUserId))
                    .tempTemplate(TempMessageTemplate.builder()
                            .titleTemplate(title)
                            .contentTemplate(content)
                            .contentFormat(ContentFormat.TEXT)
                            .build())
                    .occurredAt(LocalDateTime.now())
                    .build();
            messagePublisher.publish(command);
            log.info("消息通知已发送，messageType={}, targetUserId={}", messageType, targetUserId);
        } catch (Exception e) {
            log.error("消息通知发送失败，messageType={}, targetUserId={}", messageType, targetUserId, e);
        }
    }

    private void publishDeviceStatusChanged(Long deviceId, String deviceName,
            DeviceStatus fromStatus, DeviceStatus toStatus, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("deviceName", deviceName);
        payload.put("fromStatus", fromStatus.getCode());
        payload.put("fromStatusDesc", fromStatus.getDesc());
        payload.put("status", toStatus.getCode());
        payload.put("statusDesc", toStatus.getDesc());
        payload.put("reason", reason);
        publishRealtime("equipment.status_changed", payload);
    }

    private void publishRealtime(String eventName, Map<String, Object> payload) {
        realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
                .event(eventName)
                .source("equipment")
                .targetType(RealtimeTargetType.BROADCAST)
                .data(payload)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
