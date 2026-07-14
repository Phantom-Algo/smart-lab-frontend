package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceAlertRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceAlertRecordRepository;

@Repository
@RequiredArgsConstructor
public class DeviceAlertRecordRepositoryImpl implements DeviceAlertRecordRepository {
    private final DeviceAlertRecordMapper mapper;

    @Override
    public Optional<DeviceAlertRecordDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public IPage<DeviceAlertRecordDO> pageByCondition(Page<DeviceAlertRecordDO> page,
            Long deviceId, String paramName, Integer status) {
        LambdaQueryWrapper<DeviceAlertRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, DeviceAlertRecordDO::getDeviceId, deviceId)
               .eq(StringUtils.isNotBlank(paramName), DeviceAlertRecordDO::getParamName, paramName)
               .eq(status != null, DeviceAlertRecordDO::getStatus, status)
               .orderByDesc(DeviceAlertRecordDO::getAlertTime);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void batchInsert(List<DeviceAlertRecordDO> records) {
        for (DeviceAlertRecordDO record : records) {
            mapper.insert(record);
        }
    }

    @Override
    public int updateHandle(Long id, Long handlerId, String handleResult) {
        return mapper.update(null, new LambdaUpdateWrapper<DeviceAlertRecordDO>()
            .eq(DeviceAlertRecordDO::getId, id)
            .eq(DeviceAlertRecordDO::getStatus, 0)
            .set(DeviceAlertRecordDO::getStatus, 1)
            .set(DeviceAlertRecordDO::getHandlerId, handlerId)
            .set(DeviceAlertRecordDO::getHandleResult, handleResult));
    }
}
