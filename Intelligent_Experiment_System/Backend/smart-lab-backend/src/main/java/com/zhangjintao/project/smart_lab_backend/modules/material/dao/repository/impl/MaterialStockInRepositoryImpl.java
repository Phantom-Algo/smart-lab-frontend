package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialStockInDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialStockInMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialStockInRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MaterialStockInDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialStockInRepositoryImpl implements MaterialStockInRepository {
    private final MaterialStockInMapper mapper;

    @Override
    public Optional<MaterialStockInDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<MaterialStockInDO> findByStockInNo(String stockInNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<MaterialStockInDO>()
                .eq(MaterialStockInDO::getStockInNo, stockInNo)));
    }

    @Override
    public List<MaterialStockInDO> findByMaterialId(Long materialId) {
        return mapper.selectList(new LambdaQueryWrapper<MaterialStockInDO>()
            .eq(MaterialStockInDO::getMaterialId, materialId)
            .orderByDesc(MaterialStockInDO::getCreatedAt));
    }

    @Override
    public void insert(MaterialStockInDO entity) {
        mapper.insert(entity);
    }
}
