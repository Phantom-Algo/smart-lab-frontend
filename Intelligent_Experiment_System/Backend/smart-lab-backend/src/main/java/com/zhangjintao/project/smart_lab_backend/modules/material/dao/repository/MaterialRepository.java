package com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.MaterialDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;

/**
 * MaterialDO 对应的 Repository 接口
 *
 * @author linbowei
 */
public interface MaterialRepository {
    Optional<MaterialDO> findById(Long id);
    Optional<MaterialDO> findByMaterialNo(String materialNo);
    IPage<MaterialDO> pageByCondition(String materialName, Long labId, String materialNo, Page<MaterialDO> page);
    void insert(MaterialDO entity);
    void updateById(MaterialDO entity);
    int updateCurrentStock(Long id, int delta);
    void deleteById(Long id);

    /** 查询所有设了安全库存的物资（用于低库存检测） */
    List<MaterialDO> findAllWithSafetyStock();
}
