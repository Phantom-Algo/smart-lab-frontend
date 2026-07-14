package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairCostDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairCostRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.DeviceStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.RepairStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairReportDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.RepairVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceRepairService;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils.MessageEventNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 设备维修管理服务实现
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
public class DeviceRepairServiceImpl implements DeviceRepairService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DeviceRepairRecordRepository repairRepository;
    private final DeviceRepairCostRepository costRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceRepairRecordMapper repairMapper;
    private final EquipmentConfig config;
    private final MessagePublisher messagePublisher;
    private final PersonDirectoryPort personDirectoryPort;
    private final RealtimePublisher realtimePublisher;

    // ========== FR-0010: 故障报修 ==========

    @Override
    @Transactional
    public RepairVO reportFault(RepairReportDTO dto) {
        // 0. 校验报修人在人员管理系统中存在
        PersonDirectoryItemDTO reporter = personDirectoryPort.getByPersonId(dto.getReporterId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "报修人不存在"));

        DeviceDO device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));

        // 设备状态校验：报废设备不可报修
        if (device.getDeviceStatus() != null && device.getDeviceStatus() == DeviceStatus.SCRAPPED.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_NOT_AVAILABLE);
        }

        // 生成维修单号，创建维修记录
        DeviceRepairRecordDO repair = new DeviceRepairRecordDO();
        repair.setRepairNo(generateRepairNo());
        repair.setDeviceId(dto.getDeviceId());
        repair.setReporterId(dto.getReporterId());
        repair.setReportTime(LocalDateTime.now());
        repair.setFaultDesc(dto.getFaultDesc());
        repair.setRepairStatus(RepairStatus.PENDING.getCode());
        repairRepository.insert(repair);

        // 设备状态改为维修中
        deviceRepository.updateStatusWithVersion(dto.getDeviceId(),
                DeviceStatus.UNDER_REPAIR.getCode(), device.getVersion());
        publishRealtime("equipment.repair_reported", Map.of(
            "repairNo", repair.getRepairNo(),
            "deviceId", repair.getDeviceId(),
            "reporterId", repair.getReporterId(),
            "faultDesc", repair.getFaultDesc(),
            "repairStatus", repair.getRepairStatus(),
            "repairStatusDesc", RepairStatus.PENDING.getDesc()));
        publishDeviceStatusChanged(device, DeviceStatus.fromCode(device.getDeviceStatus()), DeviceStatus.UNDER_REPAIR,
            "设备报修");

        log.info("故障报修成功，repairNo={}, deviceId={}, reporterId={}",
                repair.getRepairNo(), dto.getDeviceId(), dto.getReporterId());
        return toVO(repair);
    }

    // ========== FR-0011: 维修处理 ==========

    @Override
    @Transactional
    public void startRepair(String repairNo, Long handlerId) {
        DeviceRepairRecordDO repair = findByRepairNoOrThrow(repairNo);
        if (repair.getRepairStatus() == null || repair.getRepairStatus() != RepairStatus.PENDING.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.REPAIR_STATUS_INVALID);
        }

        // 校验维修处理人在人员管理系统中存在
        personDirectoryPort.getByPersonId(handlerId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "维修处理人不存在"));

        repair.setRepairStatus(RepairStatus.PROCESSING.getCode());
        repair.setHandlerId(handlerId);
        repairRepository.updateById(repair);
        publishRealtime("equipment.repair_started", Map.of(
            "repairNo", repairNo,
            "deviceId", repair.getDeviceId(),
            "handlerId", handlerId,
            "repairStatus", RepairStatus.PROCESSING.getCode(),
            "repairStatusDesc", RepairStatus.PROCESSING.getDesc()));
        log.info("维修开始，repairNo={}, handlerId={}", repairNo, handlerId);
        publishRepairMessage("equipment.normal.repair.started", repair);
    }

    @Override
    @Transactional
    public RepairVO completeRepair(String repairNo, String repairDesc) {
        DeviceRepairRecordDO repair = findByRepairNoOrThrow(repairNo);
        if (repair.getRepairStatus() == null || repair.getRepairStatus() != RepairStatus.PROCESSING.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.REPAIR_STATUS_INVALID);
        }
        repair.setRepairStatus(RepairStatus.REPAIRED.getCode());
        repair.setRepairDesc(repairDesc);
        repair.setCompletedAt(LocalDateTime.now());
        repairRepository.updateById(repair);
        publishRealtime("equipment.repair_completed", Map.of(
            "repairNo", repairNo,
            "deviceId", repair.getDeviceId(),
            "repairDesc", repairDesc,
            "repairStatus", RepairStatus.REPAIRED.getCode(),
            "repairStatusDesc", RepairStatus.REPAIRED.getDesc()));
        log.info("维修完成，repairNo={}", repairNo);
        publishRepairMessage("equipment.normal.repair.completed", repair);
        return toVO(repair);
    }

    @Override
    @Transactional
    public void acceptRepair(String repairNo) {
        DeviceRepairRecordDO repair = findByRepairNoOrThrow(repairNo);
        if (repair.getRepairStatus() == null || repair.getRepairStatus() != RepairStatus.REPAIRED.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.REPAIR_STATUS_INVALID);
        }

        // 验收
        repair.setRepairStatus(RepairStatus.ACCEPTED.getCode());
        repair.setAcceptedAt(LocalDateTime.now());
        repairRepository.updateById(repair);

        // 恢复设备为闲置
        DeviceDO device = deviceRepository.findById(repair.getDeviceId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.updateStatusWithVersion(repair.getDeviceId(),
                DeviceStatus.IDLE.getCode(), device.getVersion());
        publishRealtime("equipment.repair_accepted", Map.of(
            "repairNo", repairNo,
            "deviceId", repair.getDeviceId(),
            "repairStatus", RepairStatus.ACCEPTED.getCode(),
            "repairStatusDesc", RepairStatus.ACCEPTED.getDesc()));
        publishDeviceStatusChanged(device, DeviceStatus.UNDER_REPAIR, DeviceStatus.IDLE, "维修验收完成");

        log.info("维修验收完成，repairNo={}, deviceId={}", repairNo, repair.getDeviceId());
        publishRepairMessage("equipment.normal.repair.accepted", repair);
    }

    @Override
    @Transactional
    public void recordCost(String repairNo, BigDecimal cost, String remark) {
        DeviceRepairRecordDO repair = findByRepairNoOrThrow(repairNo);

        DeviceRepairCostDO costDO = new DeviceRepairCostDO();
        costDO.setRepairId(repair.getId());
        costDO.setCost(cost);
        costDO.setRemark(remark);
        costRepository.insert(costDO);
        publishRealtime("equipment.repair_cost_recorded", Map.of(
            "repairNo", repairNo,
            "repairId", repair.getId(),
            "deviceId", repair.getDeviceId(),
            "cost", cost,
            "remark", remark));

        log.info("维修费用登记成功，repairNo={}, cost={}", repairNo, cost);
    }

    // ========== FR-0012: 维修记录查询 ==========

    @Override
    public PageResult<RepairVO> listRepairRecords(RepairQueryDTO query) {
        IPage<DeviceRepairRecordDO> page = repairRepository.pageByCondition(
                Page.of(query.getPageNum(), query.getPageSize()),
                query.getDeviceId(), query.getReporterId(), query.getRepairStatus());
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
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

    private String generateRepairNo() {
        String prefix = config.getRepairNoPrefix() + LocalDate.now().format(DATE_FORMAT);
        String maxNo = getMaxRepairNoByPrefix(prefix);
        return prefix + nextSequence(maxNo, prefix);
    }

    private String getMaxRepairNoByPrefix(String prefix) {
        List<DeviceRepairRecordDO> list = repairMapper.selectList(
                new LambdaQueryWrapper<DeviceRepairRecordDO>()
                        .likeRight(DeviceRepairRecordDO::getRepairNo, prefix)
                        .orderByDesc(DeviceRepairRecordDO::getRepairNo)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getRepairNo();
    }

    private String nextSequence(String maxNo, String prefix) {
        if (maxNo == null) return "000001";
        String seq = maxNo.substring(prefix.length());
        return String.format("%06d", Integer.parseInt(seq) + 1);
    }

    private DeviceRepairRecordDO findByRepairNoOrThrow(String repairNo) {
        return repairRepository.findByRepairNo(repairNo)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.REPAIR_NOT_FOUND));
    }

    private RepairVO toVO(DeviceRepairRecordDO repair) {
        DeviceDO device = deviceRepository.findById(repair.getDeviceId()).orElse(null);
        RepairStatus statusEnum = RepairStatus.fromCode(repair.getRepairStatus());

        // 查询费用明细
        List<RepairVO.RepairCostItem> costItems = Collections.emptyList();
        Optional<DeviceRepairCostDO> costOpt = costRepository.findByRepairId(repair.getId());
        if (costOpt.isPresent()) {
            DeviceRepairCostDO c = costOpt.get();
            costItems = List.of(RepairVO.RepairCostItem.builder()
                    .id(c.getId()).cost(c.getCost()).remark(c.getRemark()).build());
        }

        return RepairVO.builder()
                .id(repair.getId())
                .repairNo(repair.getRepairNo())
                .deviceId(repair.getDeviceId())
                .deviceName(device != null ? device.getName() : null)
                .reporterId(repair.getReporterId())
                .handlerId(repair.getHandlerId())
                .reportTime(repair.getReportTime())
                .faultDesc(repair.getFaultDesc())
                .repairStatus(repair.getRepairStatus())
                .repairStatusDesc(statusEnum != null ? statusEnum.getDesc() : null)
                .repairDesc(repair.getRepairDesc())
                .completedAt(repair.getCompletedAt())
                .acceptedAt(repair.getAcceptedAt())
                .costs(costItems)
                .createdAt(repair.getCreatedAt())
                .updatedAt(repair.getUpdatedAt())
                .build();
    }

    private void publishRepairMessage(String messageType, DeviceRepairRecordDO repair) {
        try {
            // 将 reporterId(person_id) 转换为 userId，确保消息正确送达
            Long reporterUserId = resolveUserId(repair.getReporterId());
            if (reporterUserId == null) {
                log.warn("无法找到报修人对应的用户，跳过消息发送，reporterId={}", repair.getReporterId());
                return;
            }

            DeviceDO device = deviceRepository.findById(repair.getDeviceId()).orElse(null);
            String deviceName = device != null ? device.getName() : "未知设备";

            MessagePublishCommand command = MessagePublishCommand.builder()
                    .eventNo(MessageEventNoGenerator.generate(messageType))
                    .sourceSystem("equipment")
                    .messageType(messageType)
                    .targetType(TargetType.SINGLE)
                    .targetUserIds(java.util.List.of(reporterUserId))
                    .templateCode(messageType)
                    .params(Map.of("repairNo", repair.getRepairNo(),
                            "deviceName", deviceName,
                            "repairStatus", repair.getRepairStatus() != null ? repair.getRepairStatus().toString() : ""))
                    .occurredAt(java.time.LocalDateTime.now())
                    .build();
            messagePublisher.publish(command);
            log.info("维修消息通知已发送，messageType={}, reporterId={}, userId={}",
                    messageType, repair.getReporterId(), reporterUserId);
        } catch (Exception e) {
            log.error("维修消息通知发送失败，messageType={}", messageType, e);
        }
    }

    private void publishDeviceStatusChanged(DeviceDO device, DeviceStatus fromStatus,
            DeviceStatus toStatus, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", device.getId());
        payload.put("deviceName", device.getName());
        payload.put("fromStatus", fromStatus == null ? null : fromStatus.getCode());
        payload.put("fromStatusDesc", fromStatus == null ? null : fromStatus.getDesc());
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
