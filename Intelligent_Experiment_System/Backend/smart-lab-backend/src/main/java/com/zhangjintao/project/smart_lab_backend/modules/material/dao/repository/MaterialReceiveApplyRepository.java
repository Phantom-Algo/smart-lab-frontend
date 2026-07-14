package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialReceiveApplyDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MaterialReceiveApplyDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialReceiveApplyRepository {
    Optional<MaterialReceiveApplyDO> findById(Long id);
    Optional<MaterialReceiveApplyDO> findByApplyNo(String applyNo);
    IPage<MaterialReceiveApplyDO> pageByCondition(Long materialId, Long applicantId, Integer applyStatus,
                                                   LocalDateTime startTime, LocalDateTime endTime, Page<MaterialReceiveApplyDO> page);
    void insert(MaterialReceiveApplyDO entity);
    void updateById(MaterialReceiveApplyDO entity);
    /** 查询某物资是否存在待审核的领用申请 */
    boolean existsPendingByMaterialId(Long materialId);
    /** 查询某物资+某申请人是否存在待审核的领用申请 */
    boolean existsPendingApply(Long materialId, Long applicantId);
}
