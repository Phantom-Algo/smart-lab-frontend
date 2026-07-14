package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialAlertRecordDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialAlertRecordMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialAlertRecordRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MaterialAlertRecordDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialAlertRecordRepositoryImpl implements MaterialAlertRecordRepository {
    private final MaterialAlertRecordMapper mapper;

    @Override
    public Optional<MaterialAlertRecordDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public IPage<MaterialAlertRecordDO> pageByCondition(Long materialId, Integer alertType, Integer status,
                                                         Page<MaterialAlertRecordDO> page) {
        LambdaQueryWrapper<MaterialAlertRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(materialId != null, MaterialAlertRecordDO::getMaterialId, materialId)
               .eq(alertType != null, MaterialAlertRecordDO::getAlertType, alertType)
               .eq(status != null, MaterialAlertRecordDO::getStatus, status)
               .orderByDesc(MaterialAlertRecordDO::getAlertTime);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public void insert(MaterialAlertRecordDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void updateById(MaterialAlertRecordDO entity) {
        mapper.updateById(entity);
    }

    @Override
    public boolean existsTodayAlert(Long materialId, Long batchId, Integer alertType, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        LambdaQueryWrapper<MaterialAlertRecordDO> wrapper = new LambdaQueryWrapper<MaterialAlertRecordDO>()
            .eq(MaterialAlertRecordDO::getMaterialId, materialId)
            .eq(MaterialAlertRecordDO::getAlertType, alertType)
            .ge(MaterialAlertRecordDO::getAlertTime, startOfDay)
            .lt(MaterialAlertRecordDO::getAlertTime, endOfDay);
        if (batchId != null) {
            wrapper.eq(MaterialAlertRecordDO::getBatchId, batchId);
        }
        return mapper.exists(wrapper);
    }
}
