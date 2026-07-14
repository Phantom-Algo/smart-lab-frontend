package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairCostDO;
import java.util.Optional;

/**
 * DeviceRepairCostDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceRepairCostRepository {
    Optional<DeviceRepairCostDO> findById(Long id);
    Optional<DeviceRepairCostDO> findByRepairId(Long repairId);
    void insert(DeviceRepairCostDO entity);
    void updateById(DeviceRepairCostDO entity);
}
