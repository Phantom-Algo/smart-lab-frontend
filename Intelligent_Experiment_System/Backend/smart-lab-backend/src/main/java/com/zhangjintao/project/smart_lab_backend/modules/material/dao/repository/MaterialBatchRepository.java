package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MaterialBatchDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialBatchRepository {
    Optional<MaterialBatchDO> findById(Long id);
    Optional<MaterialBatchDO> findByBatchNo(String batchNo);
    List<MaterialBatchDO> findByMaterialId(Long materialId);
    /** 查询可用批次（在库且有余量），按有效期升序（临期优先出库） */
    List<MaterialBatchDO> findAvailableByMaterialId(Long materialId);

    /** 批量查询一批物资ID下的所有批次（用于 N+1 优化） */
    List<MaterialBatchDO> findByMaterialIds(List<Long> materialIds);
    void insert(MaterialBatchDO entity);
    void updateById(MaterialBatchDO entity);
    /** CAS扣减余量：remaining >= deductAmount 才执行 */
    int deductRemaining(Long id, int deductAmount);
    int updateBatchStatus(Long id, int batchStatus);

    /**
     * 查询到期日在(startExclusive, endInclusive]范围内的可用批次
     * @param startExclusive 起始日期(不含), null表示不限制下界
     * @param endInclusive 结束日期(含), null表示不限制上界
     */
    List<MaterialBatchDO> findAvailableByExpiryRange(LocalDate startExclusive, LocalDate endInclusive);
}
