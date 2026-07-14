package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveApplyDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialReceiveApplyMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialReceiveApplyRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MaterialReceiveApplyDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialReceiveApplyRepositoryImpl implements MaterialReceiveApplyRepository {
    private final MaterialReceiveApplyMapper mapper;

    @Override
    public Optional<MaterialReceiveApplyDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<MaterialReceiveApplyDO> findByApplyNo(String applyNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<MaterialReceiveApplyDO>()
                .eq(MaterialReceiveApplyDO::getApplyNo, applyNo)));
    }

    @Override
    public IPage<MaterialReceiveApplyDO> pageByCondition(Long materialId, Long applicantId, Integer applyStatus,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          Page<MaterialReceiveApplyDO> page) {
        LambdaQueryWrapper<MaterialReceiveApplyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, MaterialReceiveApplyDO::getMaterialId, materialId)
               .eq(applicantId != null, MaterialReceiveApplyDO::getApplicantId, applicantId)
               .eq(applyStatus != null, MaterialReceiveApplyDO::getApplyStatus, applyStatus)
               .ge(startTime != null, MaterialReceiveApplyDO::getCreatedAt, startTime)
               .le(endTime != null, MaterialReceiveApplyDO::getCreatedAt, endTime)
               .orderByDesc(MaterialReceiveApplyDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(MaterialReceiveApplyDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(MaterialReceiveApplyDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public boolean existsPendingByMaterialId(Long materialId) {
        return mapper.exists(new LambdaQueryWrapper<MaterialReceiveApplyDO>()
            .eq(MaterialReceiveApplyDO::getMaterialId, materialId)
            .eq(MaterialReceiveApplyDO::getApplyStatus, 1));  // 待审核
    }

    @Override
    public boolean existsPendingApply(Long materialId, Long applicantId) {
        return mapper.exists(new LambdaQueryWrapper<MaterialReceiveApplyDO>()
            .eq(MaterialReceiveApplyDO::getMaterialId, materialId)
            .eq(MaterialReceiveApplyDO::getApplicantId, applicantId)
            .eq(MaterialReceiveApplyDO::getApplyStatus, 1));  // 待审核
    }
}
