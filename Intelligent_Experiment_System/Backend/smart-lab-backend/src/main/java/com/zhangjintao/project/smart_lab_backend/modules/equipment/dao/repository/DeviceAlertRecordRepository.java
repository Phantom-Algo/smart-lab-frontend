package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceAlertRecordDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Optional;

/**
 * DeviceAlertRecordDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceAlertRecordRepository {
    Optional<DeviceAlertRecordDO> findById(Long id);
    IPage<DeviceAlertRecordDO> pageByCondition(Page<DeviceAlertRecordDO> page, Long deviceId, String paramName, Integer status);
    void batchInsert(java.util.List<DeviceAlertRecordDO> records);
    int updateHandle(Long id, Long handlerId, String handleResult);
}
