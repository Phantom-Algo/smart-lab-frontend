package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveBatchDetailDO;

import java.util.List;

/**
 * MaterialReceiveBatchDetailDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialReceiveBatchDetailRepository {
    void insert(MaterialReceiveBatchDetailDO entity);
    void insertBatch(List<MaterialReceiveBatchDetailDO> entities);
    List<MaterialReceiveBatchDetailDO> findByReceiveLogId(Long receiveLogId);
}
