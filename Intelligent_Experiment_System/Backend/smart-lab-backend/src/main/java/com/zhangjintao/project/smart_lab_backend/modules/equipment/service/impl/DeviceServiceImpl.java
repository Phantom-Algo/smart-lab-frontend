package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.config.EquipmentConfig;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.enums.DeviceStatus;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceService;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设备台账管理服务实现
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final DeviceMapper deviceMapper;
    private final EquipmentConfig equipmentConfig;
    private final DeviceSensorBindingService deviceSensorBindingService;
    private final RealtimePublisher realtimePublisher;

    @Override
    public PageResult<DeviceVO> listDevices(DeviceQueryDTO query) {
        Page<DeviceDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<DeviceDO> result = deviceRepository.pageByCondition(query, page);
        List<DeviceVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public DeviceVO getDevice(Long id) {
        DeviceDO device = deviceRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO createDevice(DeviceCreateDTO dto) {
        // 校验设备类型存在
        equipmentTypeRepository.findById(dto.getDeviceTypeId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "设备类型不存在，typeId=" + dto.getDeviceTypeId()));

        DeviceDO device = new DeviceDO();
        device.setDeviceNo(generateDeviceNo());
        device.setName(dto.getName());
        device.setDeviceTypeId(dto.getDeviceTypeId());
        device.setLabId(dto.getLabId());
        device.setPurchaseDate(dto.getPurchaseDate());
        device.setPurchasePrice(dto.getPurchasePrice());
        device.setExpireDate(dto.getExpireDate());
        device.setRemark(dto.getRemark());
        device.setDeviceStatus(DeviceStatus.PURCHASE_PENDING.getCode());
        device.setHealthScore(100);
        device.setVersion(1);

        deviceRepository.insert(device);
        publishRealtime("equipment.stock_inbound", device);
        log.info("设备创建成功，deviceNo={}, id={}", device.getDeviceNo(), device.getId());
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO updateDevice(Long id, DeviceUpdateDTO dto) {
        DeviceDO device = deviceRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));

        if (StringUtils.hasText(dto.getName())) {
            device.setName(dto.getName());
        }
        if (dto.getDeviceTypeId() != null) {
            equipmentTypeRepository.findById(dto.getDeviceTypeId())
                    .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                            "设备类型不存在，typeId=" + dto.getDeviceTypeId()));
            device.setDeviceTypeId(dto.getDeviceTypeId());
        }
        if (dto.getLabId() != null) {
            device.setLabId(dto.getLabId());
        }
        if (dto.getPurchaseDate() != null) {
            device.setPurchaseDate(dto.getPurchaseDate());
        }
        if (dto.getPurchasePrice() != null) {
            device.setPurchasePrice(dto.getPurchasePrice());
        }
        if (dto.getExpireDate() != null) {
            device.setExpireDate(dto.getExpireDate());
        }
        if (dto.getRemark() != null) {
            device.setRemark(dto.getRemark());
        }

        deviceRepository.updateById(device);
        publishRealtime("equipment.info_updated", device);
        log.info("设备更新成功，id={}", id);
        return toVO(device);
    }

    @Override
    @Transactional
    public void deleteDevice(Long id) {
        DeviceDO device = deviceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));

        if (device.getDeviceStatus() != DeviceStatus.SCRAPPED.getCode()) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_STATUS_INVALID,
                    "仅报废状态的设备可被删除，当前状态：" + device.getDeviceStatus());
        }

        if (deviceSensorBindingService.existsActiveBindingByDeviceId(id)) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_HAS_ACTIVE_SENSOR_BINDING,
                    "设备存在有效传感器绑定，请先换绑或删除传感器，deviceId=" + id);
        }

        deviceRepository.deleteById(id);
        publishRealtime("equipment.deleted", device);
        log.info("设备删除成功，id={}, deviceNo={}", id, device.getDeviceNo());
    }

    // ========== 私有方法 ==========

    /**
     * 生成设备编号：DEV + yyyyMMdd + 3位流水号
     */
    private String generateDeviceNo() {
        String prefix = equipmentConfig.getDeviceNoPrefix() + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大编号
        List<DeviceDO> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<DeviceDO>()
                        .likeRight(DeviceDO::getDeviceNo, prefix)
                        .orderByDesc(DeviceDO::getDeviceNo)
                        .last("LIMIT 1"));
        int seq = 1;
        if (!devices.isEmpty()) {
            String maxNo = devices.get(0).getDeviceNo();
            String seqPart = maxNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("设备编号流水号解析失败，maxNo={}", maxNo);
            }
        }
        return prefix + String.format("%03d", seq);
    }

    /**
     * DeviceDO → DeviceVO 映射
     */
    private DeviceVO toVO(DeviceDO device) {
        String typeName = equipmentTypeRepository.findById(device.getDeviceTypeId())
                .map(EquipmentTypeDO::getTypeName)
                .orElse(null);

        String statusDesc = Optional.ofNullable(DeviceStatus.fromCode(device.getDeviceStatus()))
                .map(DeviceStatus::getDesc)
                .orElse(null);

        return DeviceVO.builder()
                .id(device.getId())
                .deviceNo(device.getDeviceNo())
                .name(device.getName())
                .deviceTypeId(device.getDeviceTypeId())
                .deviceTypeName(typeName)
                .labId(device.getLabId())
                .purchaseDate(device.getPurchaseDate())
                .purchasePrice(device.getPurchasePrice())
                .expireDate(device.getExpireDate())
                .deviceStatus(device.getDeviceStatus())
                .deviceStatusDesc(statusDesc)
                .healthScore(device.getHealthScore())
                .version(device.getVersion())
                .remark(device.getRemark())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    private void publishRealtime(String eventName, DeviceDO device) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", device.getId());
        payload.put("deviceNo", device.getDeviceNo());
        payload.put("deviceName", device.getName());
        payload.put("deviceTypeId", device.getDeviceTypeId());
        payload.put("labId", device.getLabId());
        payload.put("status", device.getDeviceStatus());
        payload.put("healthScore", device.getHealthScore());
        realtimePublisher.publishAfterCommit(RealtimeEvent.builder()
                .event(eventName)
                .source("equipment")
                .targetType(RealtimeTargetType.BROADCAST)
                .data(payload)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
