package com.zhangjintao.project.smart_lab_backend.modules.material.service;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialBatchDO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialBatchVO;

import java.util.List;

/**
 * 批次管理服务接口 — FR-0006
 *
 * @author linbowei
 */
public interface MaterialBatchService {

    /** 查看指定物资的所有批次（FR-0006） */
    List<MaterialBatchVO> listBatchesByMaterial(Long materialId);

    /** 管理员手动将批次标记为报废（FR-0006） */
    void scrapBatch(Long batchId, Long operatorId);

    /** 批次DO→VO转换 */
    MaterialBatchVO toBatchVO(MaterialBatchDO batch);
}
