package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDataMonitorLogDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * DeviceDataMonitorLogDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceDataMonitorLogRepository {
    IPage<DeviceDataMonitorLogDO> pageByCondition(Page<DeviceDataMonitorLogDO> page, Long deviceId, String metricName, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
    void batchInsert(java.util.List<DeviceDataMonitorLogDO> logs);
}
