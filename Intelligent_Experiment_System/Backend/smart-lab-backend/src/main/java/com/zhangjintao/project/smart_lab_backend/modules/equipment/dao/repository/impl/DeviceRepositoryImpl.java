package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceQueryDTO;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {
    private final DeviceMapper mapper;

    @Override
    public Optional<DeviceDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<DeviceDO> findByIdForUpdate(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<DeviceDO>()
            .eq(DeviceDO::getId, id)
            .last("LIMIT 1 FOR UPDATE")));
    }

    @Override
    public Optional<DeviceDO> findByDeviceNo(String deviceNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<DeviceDO>()
                .eq(DeviceDO::getDeviceNo, deviceNo)));
    }

    @Override
    public List<DeviceDO> findByLabId(Long labId) {
        return mapper.selectList(new LambdaQueryWrapper<DeviceDO>()
            .eq(DeviceDO::getLabId, labId));
    }

    @Override
    public List<DeviceDO> findByDeviceTypeId(Long deviceTypeId) {
        return mapper.selectList(new LambdaQueryWrapper<DeviceDO>()
            .eq(DeviceDO::getDeviceTypeId, deviceTypeId));
    }

    @Override
    public IPage<DeviceDO> pageByCondition(DeviceQueryDTO query, Page<DeviceDO> page) {
        LambdaQueryWrapper<DeviceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getName()), DeviceDO::getName, query.getName())
               .eq(query.getDeviceTypeId() != null, DeviceDO::getDeviceTypeId, query.getDeviceTypeId())
               .eq(query.getLabId() != null, DeviceDO::getLabId, query.getLabId())
               .eq(query.getDeviceStatus() != null, DeviceDO::getDeviceStatus, query.getDeviceStatus())
               .orderByDesc(DeviceDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(DeviceDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(DeviceDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int updateStatusWithVersion(Long id, Integer deviceStatus, Integer version) {
        return mapper.update(null, new LambdaUpdateWrapper<DeviceDO>()
            .eq(DeviceDO::getId, id)
            .eq(DeviceDO::getVersion, version)
            .set(DeviceDO::getDeviceStatus, deviceStatus)
            .setSql("version = version + 1"));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
