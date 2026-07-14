package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentMetricIndicatorDO;
import java.util.List;
import java.util.Optional;

/**
 * EquipmentMetricIndicatorDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface EquipmentMetricIndicatorRepository {
    Optional<EquipmentMetricIndicatorDO> findById(Long id);
    List<EquipmentMetricIndicatorDO> findByEquipmentTypeId(Long typeId);
    Optional<EquipmentMetricIndicatorDO> findByTypeIdAndMetricName(Long typeId, String metricName);
    void insert(EquipmentMetricIndicatorDO entity);
    void updateById(EquipmentMetricIndicatorDO entity);
    void deleteById(Long id);
}
