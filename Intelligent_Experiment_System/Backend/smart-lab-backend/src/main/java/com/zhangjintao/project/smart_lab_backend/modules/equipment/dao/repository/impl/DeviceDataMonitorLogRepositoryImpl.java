package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDataMonitorLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceDataMonitorLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceDataMonitorLogRepository;

@Repository
@RequiredArgsConstructor
public class DeviceDataMonitorLogRepositoryImpl implements DeviceDataMonitorLogRepository {
    private final DeviceDataMonitorLogMapper mapper;

    @Override
    public IPage<DeviceDataMonitorLogDO> pageByCondition(Page<DeviceDataMonitorLogDO> page,
            Long deviceId, String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<DeviceDataMonitorLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, DeviceDataMonitorLogDO::getDeviceId, deviceId)
               .ge(startTime != null, DeviceDataMonitorLogDO::getDetectedAt, startTime)
               .le(endTime != null, DeviceDataMonitorLogDO::getDetectedAt, endTime)
               .orderByDesc(DeviceDataMonitorLogDO::getDetectedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void batchInsert(List<DeviceDataMonitorLogDO> logs) {
        for (DeviceDataMonitorLogDO log : logs) {
            mapper.insert(log);
        }
    }
}
