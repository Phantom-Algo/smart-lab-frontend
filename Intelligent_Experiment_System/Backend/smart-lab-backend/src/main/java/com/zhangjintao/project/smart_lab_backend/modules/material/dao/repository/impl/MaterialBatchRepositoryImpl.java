package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialBatchMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import java.util.List;
import java.util.Optional;

/**
 * MaterialBatchDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialBatchRepositoryImpl implements MaterialBatchRepository {
    private final MaterialBatchMapper mapper;

    @Override
    public Optional<MaterialBatchDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<MaterialBatchDO> findByBatchNo(String batchNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<MaterialBatchDO>()
                .eq(MaterialBatchDO::getBatchNo, batchNo)));
    }

    @Override
    public List<MaterialBatchDO> findByMaterialId(Long materialId) {
        return mapper.selectList(new LambdaQueryWrapper<MaterialBatchDO>()
            .eq(MaterialBatchDO::getMaterialId, materialId));
    }

    @Override
    public List<MaterialBatchDO> findByMaterialIds(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<MaterialBatchDO>()
            .in(MaterialBatchDO::getMaterialId, materialIds));
    }

    @Override
    public List<MaterialBatchDO> findAvailableByMaterialId(Long materialId) {
        return mapper.selectList(new LambdaQueryWrapper<MaterialBatchDO>()
            .eq(MaterialBatchDO::getMaterialId, materialId)
            .eq(MaterialBatchDO::getBatchStatus, 1)  // 在库可用
            .gt(MaterialBatchDO::getRemaining, 0)    // 有余量
            .orderByAsc(MaterialBatchDO::getExpiryDate)   // 临期优先
            .orderByAsc(MaterialBatchDO::getStockInTime)); // 同效期FIFO：先入库先出
    }

    @Override
    public void insert(MaterialBatchDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(MaterialBatchDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int deductRemaining(Long id, int deductAmount) {
        return mapper.update(null, new LambdaUpdateWrapper<MaterialBatchDO>()
            .eq(MaterialBatchDO::getId, id)
            .ge(MaterialBatchDO::getRemaining, deductAmount)
            .setSql("remaining = remaining - {0}", deductAmount));
    }

    @Override
    public int updateBatchStatus(Long id, int batchStatus) {
        return mapper.update(null, new LambdaUpdateWrapper<MaterialBatchDO>()
            .eq(MaterialBatchDO::getId, id)
            .set(MaterialBatchDO::getBatchStatus, batchStatus));
    }

    @Override
    public List<MaterialBatchDO> findAvailableByExpiryRange(LocalDate startExclusive, LocalDate endInclusive) {
        LambdaQueryWrapper<MaterialBatchDO> wrapper = new LambdaQueryWrapper<MaterialBatchDO>()
                .eq(MaterialBatchDO::getBatchStatus, 1)
                .gt(MaterialBatchDO::getRemaining, 0);
        if (startExclusive != null) {
            wrapper.gt(MaterialBatchDO::getExpiryDate, startExclusive);
        }
        if (endInclusive != null) {
            wrapper.le(MaterialBatchDO::getExpiryDate, endInclusive);
        } else if (startExclusive != null) {
            // 无上界时仅约束下界
        }
        return mapper.selectList(wrapper);
    }
}
