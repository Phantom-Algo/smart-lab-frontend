package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.EquipmentTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentTypeRepository;

@Repository
@RequiredArgsConstructor
public class EquipmentTypeRepositoryImpl implements EquipmentTypeRepository {
    private final EquipmentTypeMapper mapper;

    @Override
    public Optional<EquipmentTypeDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<EquipmentTypeDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentTypeDO>()
            .orderByAsc(EquipmentTypeDO::getTypeName));
    }

    @Override
    public Optional<EquipmentTypeDO> findByTypeName(String typeName) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<EquipmentTypeDO>()
                .eq(EquipmentTypeDO::getTypeName, typeName)));
    }

    @Override
    public void insert(EquipmentTypeDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(EquipmentTypeDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
