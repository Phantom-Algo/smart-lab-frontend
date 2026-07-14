package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

/**
 * MaterialDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialRepositoryImpl implements MaterialRepository {
    private final MaterialMapper mapper;

    @Override
    public Optional<MaterialDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<MaterialDO> findByMaterialNo(String materialNo) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<MaterialDO>()
                .eq(MaterialDO::getMaterialNo, materialNo)));
    }

    @Override
    public IPage<MaterialDO> pageByCondition(String materialName, Long labId, String materialNo, Page<MaterialDO> page) {
        LambdaQueryWrapper<MaterialDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(materialName), MaterialDO::getMaterialName, materialName)
               .eq(labId != null, MaterialDO::getLabId, labId)
               .eq(StringUtils.isNotBlank(materialNo), MaterialDO::getMaterialNo, materialNo)
               .orderByDesc(MaterialDO::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(MaterialDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(MaterialDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public int updateCurrentStock(Long id, int delta) {
        LambdaUpdateWrapper<MaterialDO> wrapper = new LambdaUpdateWrapper<MaterialDO>()
            .eq(MaterialDO::getId, id)
            .setSql("current_stock = current_stock + {0}", delta);
        // CAS 保护：扣减时确保 current_stock >= |delta|，防止并发导致负库存
        if (delta < 0) {
            wrapper.ge(MaterialDO::getCurrentStock, -delta);
        }
        return mapper.update(null, wrapper);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<MaterialDO> findAllWithSafetyStock() {
        return mapper.selectList(new LambdaQueryWrapper<MaterialDO>()
                .gt(MaterialDO::getSafetyStock, 0));
    }
}
