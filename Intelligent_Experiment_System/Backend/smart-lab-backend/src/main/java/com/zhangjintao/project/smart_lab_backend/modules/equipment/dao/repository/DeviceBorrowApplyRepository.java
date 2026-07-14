package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceBorrowApplyDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Optional;

/**
 * DeviceBorrowApplyDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface DeviceBorrowApplyRepository {
    Optional<DeviceBorrowApplyDO> findById(Long id);
    Optional<DeviceBorrowApplyDO> findByApplyNo(String applyNo);
    IPage<DeviceBorrowApplyDO> pageByCondition(Page<DeviceBorrowApplyDO> page, String applyNo, Long deviceId, Long applicantId, Integer applyStatus);
    List<DeviceBorrowApplyDO> findByDeviceIdAndStatus(Long deviceId, Integer applyStatus);
    boolean existsPendingApply(Long deviceId, Long applicantId);
    void insert(DeviceBorrowApplyDO entity);
    void updateById(DeviceBorrowApplyDO entity);
    int updateStatus(String applyNo, Integer applyStatus, Long approverId, String rejectReason);
}
