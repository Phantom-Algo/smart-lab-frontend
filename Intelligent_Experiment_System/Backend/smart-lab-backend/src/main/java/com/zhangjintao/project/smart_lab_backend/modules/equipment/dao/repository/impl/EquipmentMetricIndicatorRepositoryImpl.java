package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.EquipmentMetricIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.EquipmentMetricIndicatorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.EquipmentMetricIndicatorRepository;

@Repository
@RequiredArgsConstructor
public class EquipmentMetricIndicatorRepositoryImpl implements EquipmentMetricIndicatorRepository {
    private final EquipmentMetricIndicatorMapper mapper;

    @Override
    public Optional<EquipmentMetricIndicatorDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<EquipmentMetricIndicatorDO> findByEquipmentTypeId(Long typeId) {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentMetricIndicatorDO>()
            .eq(EquipmentMetricIndicatorDO::getEquipmentTypeId, typeId));
    }

    @Override
    public Optional<EquipmentMetricIndicatorDO> findByTypeIdAndMetricName(Long typeId, String metricName) {
        return Optional.ofNullable(mapper.selectOne(
            new LambdaQueryWrapper<EquipmentMetricIndicatorDO>()
                .eq(EquipmentMetricIndicatorDO::getEquipmentTypeId, typeId)
                .eq(EquipmentMetricIndicatorDO::getMetricName, metricName)));
    }

    @Override
    public void insert(EquipmentMetricIndicatorDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(EquipmentMetricIndicatorDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
