package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialAlertRecordDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.Optional;

/**
 * MaterialAlertRecordDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialAlertRecordRepository {
    Optional<MaterialAlertRecordDO> findById(Long id);
    IPage<MaterialAlertRecordDO> pageByCondition(Long materialId, Integer alertType, Integer status, Page<MaterialAlertRecordDO> page);
    void insert(MaterialAlertRecordDO entity);
    void updateById(MaterialAlertRecordDO entity);
    /** 查询当日是否已有同物资+同批次+同类型的预警（幂等） */
    boolean existsTodayAlert(Long materialId, Long batchId, Integer alertType, LocalDate today);
}
