package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceRepairRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepairRecordRepository;

@Repository
@RequiredArgsConstructor
public class DeviceRepairRecordRepositoryImpl implements DeviceRepairRecordRepository {
    private final DeviceRepairRecordMapper mapper;

    @Override
    public Optional<DeviceRepairRecordDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<DeviceRepairRecordDO> findByRepairNo(String repairNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<DeviceRepairRecordDO>()
                .eq(DeviceRepairRecordDO::getRepairNo, repairNo)));
    }

    @Override
    public IPage<DeviceRepairRecordDO> pageByCondition(Page<DeviceRepairRecordDO> page,
            Long deviceId, Long reporterId, Integer repairStatus) {
        LambdaQueryWrapper<DeviceRepairRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, DeviceRepairRecordDO::getDeviceId, deviceId)
               .eq(reporterId != null, DeviceRepairRecordDO::getReporterId, reporterId)
               .eq(repairStatus != null, DeviceRepairRecordDO::getRepairStatus, repairStatus)
               .orderByDesc(DeviceRepairRecordDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(DeviceRepairRecordDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(DeviceRepairRecordDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int updateStatus(String repairNo, Integer repairStatus) {
        return mapper.update(null, new LambdaUpdateWrapper<DeviceRepairRecordDO>()
            .eq(DeviceRepairRecordDO::getRepairNo, repairNo)
            .set(DeviceRepairRecordDO::getRepairStatus, repairStatus));
    }
}
