package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowLogDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Optional;

/**
 * DeviceBorrowLogDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceBorrowLogRepository {
    Optional<DeviceBorrowLogDO> findById(Long id);
    Optional<DeviceBorrowLogDO> findByApplyId(Long applyId);
    IPage<DeviceBorrowLogDO> pageByCondition(Page<DeviceBorrowLogDO> page, Long deviceId, Long borrowerId, Integer status);
    List<DeviceBorrowLogDO> findOverdueLogs();
    void insert(DeviceBorrowLogDO entity);
    void updateById(DeviceBorrowLogDO entity);
    int updateToReturned(Long id, Long returnOperatorId, String returnRemark, Integer isOverdue);
}
