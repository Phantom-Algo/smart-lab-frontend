package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceQueryDTO;
import java.util.List;
import java.util.Optional;

/**
 * DeviceDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceRepository {
    Optional<DeviceDO> findById(Long id);
    Optional<DeviceDO> findByIdForUpdate(Long id);
    Optional<DeviceDO> findByDeviceNo(String deviceNo);
    List<DeviceDO> findByLabId(Long labId);
    List<DeviceDO> findByDeviceTypeId(Long deviceTypeId);
    IPage<DeviceDO> pageByCondition(DeviceQueryDTO query, Page<DeviceDO> page);
    void insert(DeviceDO entity);
    void updateById(DeviceDO entity);
    int updateStatusWithVersion(Long id, Integer deviceStatus, Integer version);
    void deleteById(Long id);
}
