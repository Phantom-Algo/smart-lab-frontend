package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import java.util.List;
import java.util.Optional;

/**
 * EquipmentTypeDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface EquipmentTypeRepository {
    Optional<EquipmentTypeDO> findById(Long id);
    List<EquipmentTypeDO> findAll();
    Optional<EquipmentTypeDO> findByTypeName(String typeName);
    void insert(EquipmentTypeDO entity);
    void updateById(EquipmentTypeDO entity);
    void deleteById(Long id);
}
