package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairCostDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairCostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairCostRepository;

@Repository
@RequiredArgsConstructor
public class DeviceRepairCostRepositoryImpl implements DeviceRepairCostRepository {
    private final DeviceRepairCostMapper mapper;

    @Override
    public Optional<DeviceRepairCostDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<DeviceRepairCostDO> findByRepairId(Long repairId) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<DeviceRepairCostDO>()
                .eq(DeviceRepairCostDO::getRepairId, repairId)));
    }

    @Override
    public void insert(DeviceRepairCostDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(DeviceRepairCostDO entity) {
        mapper.updateById(entity);
    }
}
