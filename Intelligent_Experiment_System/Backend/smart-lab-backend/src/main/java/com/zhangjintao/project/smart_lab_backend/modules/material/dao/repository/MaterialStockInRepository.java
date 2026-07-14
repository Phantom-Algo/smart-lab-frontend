package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialStockInDO;

import java.util.List;
import java.util.Optional;

/**
 * MaterialStockInDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialStockInRepository {
    Optional<MaterialStockInDO> findById(Long id);
    Optional<MaterialStockInDO> findByStockInNo(String stockInNo);
    List<MaterialStockInDO> findByMaterialId(Long materialId);
    void insert(MaterialStockInDO entity);
}
