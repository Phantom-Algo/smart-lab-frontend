package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialReceiveLogMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialReceiveLogRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MaterialReceiveLogDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialReceiveLogRepositoryImpl implements MaterialReceiveLogRepository {
    private final MaterialReceiveLogMapper mapper;

    @Override
    public Optional<MaterialReceiveLogDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<MaterialReceiveLogDO> findByApplyId(Long applyId) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<MaterialReceiveLogDO>()
                .eq(MaterialReceiveLogDO::getApplyId, applyId)));
    }

    @Override
    public IPage<MaterialReceiveLogDO> pageByCondition(Long materialId, Long receiverId,
                                                        LocalDateTime startTime, LocalDateTime endTime,
                                                        Page<MaterialReceiveLogDO> page) {
        LambdaQueryWrapper<MaterialReceiveLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, MaterialReceiveLogDO::getMaterialId, materialId)
               .eq(receiverId != null, MaterialReceiveLogDO::getReceiverId, receiverId)
               .ge(startTime != null, MaterialReceiveLogDO::getOutTime, startTime)
               .le(endTime != null, MaterialReceiveLogDO::getOutTime, endTime)
               .orderByDesc(MaterialReceiveLogDO::getOutTime);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(MaterialReceiveLogDO entity) {
        mapper.insert(entity);
    }
}
