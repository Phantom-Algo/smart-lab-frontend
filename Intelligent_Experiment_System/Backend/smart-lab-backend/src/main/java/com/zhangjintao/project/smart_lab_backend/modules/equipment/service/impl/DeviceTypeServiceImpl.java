package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceTypeCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceTypeUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceTypeVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备类型管理服务实现
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final EquipmentTypeRepository equipmentTypeRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public List<DeviceTypeVO> listTypes() {
        return equipmentTypeRepository.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceTypeVO getType(Long id) {
        EquipmentTypeDO type = equipmentTypeRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "设备类型不存在，id=" + id));
        return toVO(type);
    }

    @Override
    @Transactional
    public DeviceTypeVO createType(DeviceTypeCreateDTO dto) {
        // 校验类型名唯一
        equipmentTypeRepository.findByTypeName(dto.getTypeName()).ifPresent(t -> {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_NO_DUPLICATE,
                    "设备类型名称已存在：" + dto.getTypeName());
        });

        EquipmentTypeDO type = new EquipmentTypeDO();
        type.setTypeName(dto.getTypeName());
        type.setDescription(dto.getDescription());
        equipmentTypeRepository.insert(type);
        log.info("设备类型创建成功，typeName={}, id={}", type.getTypeName(), type.getId());
        return toVO(type);
    }

    @Override
    @Transactional
    public DeviceTypeVO updateType(Long id, DeviceTypeUpdateDTO dto) {
        EquipmentTypeDO type = equipmentTypeRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "设备类型不存在，id=" + id));

        if (StringUtils.hasText(dto.getTypeName())) {
            // 校验名称唯一（排除自身）
            equipmentTypeRepository.findByTypeName(dto.getTypeName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new EquipmentException(EquipmentErrorCode.DEVICE_NO_DUPLICATE,
                            "设备类型名称已存在：" + dto.getTypeName());
                }
            });
            type.setTypeName(dto.getTypeName());
        }
        if (dto.getDescription() != null) {
            type.setDescription(dto.getDescription());
        }

        equipmentTypeRepository.updateById(type);
        log.info("设备类型更新成功，id={}", id);
        return toVO(type);
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        EquipmentTypeDO type = equipmentTypeRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "设备类型不存在，id=" + id));

        // 校验无设备引用此类型
        if (!deviceRepository.findByDeviceTypeId(id).isEmpty()) {
            throw new EquipmentException(EquipmentErrorCode.DEVICE_STATUS_INVALID,
                    "该设备类型下存在设备，不可删除，typeId=" + id);
        }

        equipmentTypeRepository.deleteById(id);
        log.info("设备类型删除成功，id={}, typeName={}", id, type.getTypeName());
    }

    // ========== 私有方法 ==========

    private DeviceTypeVO toVO(EquipmentTypeDO type) {
        return DeviceTypeVO.builder()
                .id(type.getId())
                .typeName(type.getTypeName())
                .description(type.getDescription())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
}
