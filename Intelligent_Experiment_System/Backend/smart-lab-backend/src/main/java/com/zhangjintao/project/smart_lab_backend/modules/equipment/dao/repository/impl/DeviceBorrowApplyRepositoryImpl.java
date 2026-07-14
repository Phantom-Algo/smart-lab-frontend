package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowApplyDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceBorrowApplyMapper;
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
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceBorrowApplyRepository;

@Repository
@RequiredArgsConstructor
public class DeviceBorrowApplyRepositoryImpl implements DeviceBorrowApplyRepository {
    private final DeviceBorrowApplyMapper mapper;

    @Override
    public Optional<DeviceBorrowApplyDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<DeviceBorrowApplyDO> findByApplyNo(String applyNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<DeviceBorrowApplyDO>()
                .eq(DeviceBorrowApplyDO::getApplyNo, applyNo)));
    }

    @Override
    public IPage<DeviceBorrowApplyDO> pageByCondition(Page<DeviceBorrowApplyDO> page,
            String applyNo, Long deviceId, Long applicantId, Integer applyStatus) {
        LambdaQueryWrapper<DeviceBorrowApplyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(applyNo), DeviceBorrowApplyDO::getApplyNo, applyNo)
               .eq(deviceId != null, DeviceBorrowApplyDO::getDeviceId, deviceId)
               .eq(applicantId != null, DeviceBorrowApplyDO::getApplicantId, applicantId)
               .eq(applyStatus != null, DeviceBorrowApplyDO::getApplyStatus, applyStatus)
               .orderByDesc(DeviceBorrowApplyDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public List<DeviceBorrowApplyDO> findByDeviceIdAndStatus(Long deviceId, Integer applyStatus) {
        return mapper.selectList(new LambdaQueryWrapper<DeviceBorrowApplyDO>()
            .eq(DeviceBorrowApplyDO::getDeviceId, deviceId)
            .eq(DeviceBorrowApplyDO::getApplyStatus, applyStatus));
    }

    @Override
    public boolean existsPendingApply(Long deviceId, Long applicantId) {
        return mapper.selectCount(new LambdaQueryWrapper<DeviceBorrowApplyDO>()
            .eq(DeviceBorrowApplyDO::getDeviceId, deviceId)
            .eq(DeviceBorrowApplyDO::getApplicantId, applicantId)
            .eq(DeviceBorrowApplyDO::getApplyStatus, 1)) > 0;
    }

    @Override
    public void insert(DeviceBorrowApplyDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(DeviceBorrowApplyDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int updateStatus(String applyNo, Integer applyStatus, Long approverId, String rejectReason) {
        LambdaUpdateWrapper<DeviceBorrowApplyDO> wrapper = new LambdaUpdateWrapper<DeviceBorrowApplyDO>()
            .eq(DeviceBorrowApplyDO::getApplyNo, applyNo)
            .set(DeviceBorrowApplyDO::getApplyStatus, applyStatus)
            .set(DeviceBorrowApplyDO::getUpdatedAt, LocalDateTime.now());
        if (approverId != null) {
            wrapper.set(DeviceBorrowApplyDO::getApproverId, approverId);
        }
        if (rejectReason != null) {
            wrapper.set(DeviceBorrowApplyDO::getRejectReason, rejectReason);
        }
        return mapper.update(null, wrapper);
    }
}
