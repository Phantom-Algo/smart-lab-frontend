package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceBorrowLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowLogRepository;

@Repository
@RequiredArgsConstructor
public class DeviceBorrowLogRepositoryImpl implements DeviceBorrowLogRepository {
    private final DeviceBorrowLogMapper mapper;

    @Override
    public Optional<DeviceBorrowLogDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<DeviceBorrowLogDO> findByApplyId(Long applyId) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<DeviceBorrowLogDO>()
                .eq(DeviceBorrowLogDO::getApplyId, applyId)));
    }

    @Override
    public IPage<DeviceBorrowLogDO> pageByCondition(Page<DeviceBorrowLogDO> page,
            Long deviceId, Long borrowerId, Integer status) {
        LambdaQueryWrapper<DeviceBorrowLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, DeviceBorrowLogDO::getDeviceId, deviceId)
               .eq(borrowerId != null, DeviceBorrowLogDO::getBorrowerId, borrowerId)
               .eq(status != null, DeviceBorrowLogDO::getStatus, status)
               .orderByDesc(DeviceBorrowLogDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public List<DeviceBorrowLogDO> findOverdueLogs() {
        return mapper.selectList(new LambdaQueryWrapper<DeviceBorrowLogDO>()
            .eq(DeviceBorrowLogDO::getStatus, 1)
            .lt(DeviceBorrowLogDO::getExpectedReturnTime, LocalDateTime.now()));
    }

    @Override
    public void insert(DeviceBorrowLogDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(DeviceBorrowLogDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int updateToReturned(Long id, Long returnOperatorId, String returnRemark, Integer isOverdue) {
        return mapper.update(null, new LambdaUpdateWrapper<DeviceBorrowLogDO>()
            .eq(DeviceBorrowLogDO::getId, id)
            .set(DeviceBorrowLogDO::getStatus, 2)
            .set(DeviceBorrowLogDO::getActualReturnTime, LocalDateTime.now())
            .set(DeviceBorrowLogDO::getReturnOperatorId, returnOperatorId)
            .set(DeviceBorrowLogDO::getReturnRemark, returnRemark)
            .set(DeviceBorrowLogDO::getIsOverdue, isOverdue));
    }
}
