package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentMetricIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentMetricIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.MetricIndicatorConfigDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.MetricIndicatorVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设备监测指标管理服务实现
 *
 * @author linbowei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceMetricServiceImpl implements DeviceMetricService {

    private final EquipmentMetricIndicatorRepository metricIndicatorRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    @Override
    public List<MetricIndicatorVO> getMetricsByTypeId(Long typeId) {
        return metricIndicatorRepository.findByEquipmentTypeId(typeId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MetricIndicatorVO configureMetric(MetricIndicatorConfigDTO dto) {
        // 校验设备类型存在
        equipmentTypeRepository.findById(dto.getEquipmentTypeId())
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "设备类型不存在，typeId=" + dto.getEquipmentTypeId()));

        // 查是否已存在同名指标
        Optional<EquipmentMetricIndicatorDO> existing = metricIndicatorRepository
                .findByTypeIdAndMetricName(dto.getEquipmentTypeId(), dto.getMetricName());

        if (existing.isPresent()) {
            // 更新
            EquipmentMetricIndicatorDO indicator = existing.get();
            if (StringUtils.hasText(dto.getUnit())) {
                indicator.setUnit(dto.getUnit());
            }
            if (dto.getSafeScope() != null) {
                indicator.setSafeScope(dto.getSafeScope());
            }
            metricIndicatorRepository.updateById(indicator);
            log.info("监测指标更新成功，typeId={}, metricName={}", dto.getEquipmentTypeId(), dto.getMetricName());
            return toVO(indicator);
        } else {
            // 新增
            EquipmentMetricIndicatorDO indicator = new EquipmentMetricIndicatorDO();
            indicator.setEquipmentTypeId(dto.getEquipmentTypeId());
            indicator.setMetricName(dto.getMetricName());
            indicator.setUnit(dto.getUnit());
            indicator.setSafeScope(dto.getSafeScope());
            metricIndicatorRepository.insert(indicator);
            log.info("监测指标配置成功，typeId={}, metricName={}, id={}",
                    dto.getEquipmentTypeId(), dto.getMetricName(), indicator.getId());
            return toVO(indicator);
        }
    }

    @Override
    @Transactional
    public void deleteMetric(Long id) {
        EquipmentMetricIndicatorDO indicator = metricIndicatorRepository.findById(id)
                .orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND,
                        "监测指标不存在，id=" + id));
        metricIndicatorRepository.deleteById(id);
        log.info("监测指标删除成功，id={}, metricName={}", id, indicator.getMetricName());
    }

    // ========== 私有方法 ==========

    private MetricIndicatorVO toVO(EquipmentMetricIndicatorDO indicator) {
        return MetricIndicatorVO.builder()
                .id(indicator.getId())
                .equipmentTypeId(indicator.getEquipmentTypeId())
                .metricName(indicator.getMetricName())
                .unit(indicator.getUnit())
                .safeScope(indicator.getSafeScope())
                .createdAt(indicator.getCreatedAt())
                .updatedAt(indicator.getUpdatedAt())
                .build();
    }
}
