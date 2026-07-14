package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceAlertRecordRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.AlertStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.AlertRecordVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceAlertService;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备告警管理服务实现
 * <p>
 * handlerId 通过 {@link PersonDirectoryPort} 校验在人员管理系统中存在。
 * </p>
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAlertServiceImpl implements DeviceAlertService {

    private final DeviceAlertRecordRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final PersonDirectoryPort personDirectoryPort;

    @Override
    public PageResult<AlertRecordVO> listAlerts(AlertQueryDTO query) {
        IPage<DeviceAlertRecordDO> page = alertRepository.pageByCondition(
                Page.of(query.getPageNum(), query.getPageSize()),
                query.getDeviceId(), query.getParamName(), query.getStatus());
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void handleAlert(Long id, Long handlerId, String handleResult) {
        // 校验处理人在人员管理系统中存在
        personDirectoryPort.getByPersonId(handlerId)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "告警处理人不存在"));

        DeviceAlertRecordDO alert = alertRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.ALERT_NOT_FOUND));
        if (alert.getStatus() != null && alert.getStatus() == AlertStatus.HANDLED.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.PARAM_INVALID, "告警已处理，不可重复操作");
        }
        alertRepository.updateHandle(id, handlerId, handleResult);
        log.info("告警处理完成，id={}, handlerId={}", id, handlerId);
    }

    private AlertRecordVO toVO(DeviceAlertRecordDO record) {
        String deviceName = deviceRepository.findById(record.getDeviceId())
                .map(d -> d.getName()).orElse(null);
        AlertStatus statusEnum = AlertStatus.fromCode(record.getStatus());
        return AlertRecordVO.builder()
                .id(record.getId()).deviceId(record.getDeviceId()).deviceName(deviceName)
                .sensorId(record.getSensorId()).paramName(record.getParamName())
                .actualValue(record.getActualValue()).safeScope(record.getSafeScope())
                .exceedDirection(record.getExceedDirection()).alertTime(record.getAlertTime())
                .status(record.getStatus())
                .statusDesc(statusEnum != null ? statusEnum.getDesc() : null)
                .handlerId(record.getHandlerId()).handleResult(record.getHandleResult())
                .createdAt(record.getCreatedAt()).build();
    }
}
