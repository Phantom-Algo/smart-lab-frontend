package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveBatchDetailDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.mapper.MaterialReceiveBatchDetailMapper;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.MaterialReceiveBatchDetailRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MaterialReceiveBatchDetailDO 对应的 Repository 实现
 *
 * @author linbowei
 */
@Repository
@RequiredArgsConstructor
public class MaterialReceiveBatchDetailRepositoryImpl implements MaterialReceiveBatchDetailRepository {
    private final MaterialReceiveBatchDetailMapper mapper;

    @Override
    public void insert(MaterialReceiveBatchDetailDO entity) {
        mapper.insert(entity);
    }

    @Override
    public void insertBatch(List<MaterialReceiveBatchDetailDO> entities) {
        if (entities != null && !entities.isEmpty()) {
            SqlHelper.executeBatch(
                MaterialReceiveBatchDetailDO.class,
                null,
                entities,
                entities.size(),
                (sqlSession, entity) -> sqlSession.getMapper(MaterialReceiveBatchDetailMapper.class).insert(entity)
            );
        }
    }

    @Override
    public List<MaterialReceiveBatchDetailDO> findByReceiveLogId(Long receiveLogId) {
        return mapper.selectList(new LambdaQueryWrapper<MaterialReceiveBatchDetailDO>()
            .eq(MaterialReceiveBatchDetailDO::getReceiveLogId, receiveLogId));
    }
}
