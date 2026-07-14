package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveLogDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MaterialReceiveLogDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialReceiveLogRepository {
    Optional<MaterialReceiveLogDO> findById(Long id);
    Optional<MaterialReceiveLogDO> findByApplyId(Long applyId);
    IPage<MaterialReceiveLogDO> pageByCondition(Long materialId, Long receiverId,
                                                 LocalDateTime startTime, LocalDateTime endTime, Page<MaterialReceiveLogDO> page);
    void insert(MaterialReceiveLogDO entity);
}
