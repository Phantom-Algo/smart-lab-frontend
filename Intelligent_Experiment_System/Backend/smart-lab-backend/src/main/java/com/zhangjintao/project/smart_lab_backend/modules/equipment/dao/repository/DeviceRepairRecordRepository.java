package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceRepairRecordDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Optional;

/**
 * DeviceRepairRecordDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceRepairRecordRepository {
    Optional<DeviceRepairRecordDO> findById(Long id);
    Optional<DeviceRepairRecordDO> findByRepairNo(String repairNo);
    IPage<DeviceRepairRecordDO> pageByCondition(Page<DeviceRepairRecordDO> page, Long deviceId, Long reporterId, Integer repairStatus);
    void insert(DeviceRepairRecordDO entity);
    void updateById(DeviceRepairRecordDO entity);
    int updateStatus(String repairNo, Integer repairStatus);
}
