package com.zhangjintao.project.smart_lab_backend.modules.material.service;

import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.StockInDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialDetailVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.StockInResultVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 物资管理服务接口 — FR-0001, FR-0002
 *
 * @author linbowei
 */
public interface MaterialService {

    /** 物资分页查询（多条件）（FR-0001） */
    PageResult<MaterialVO> pageMaterials(MaterialQueryDTO query);

    /** 物资详情含批次列表（FR-0001） */
    MaterialDetailVO getMaterialDetail(Long id);

    /** 新增物资，自动生成编号（FR-0002） */
    MaterialVO createMaterial(MaterialCreateDTO dto, Long operatorId);

    /** 更新物资基本信息（FR-0002） */
    MaterialVO updateMaterial(Long id, MaterialUpdateDTO dto);

    /** 删除物资（校验库存和进行中申请）（FR-0002） */
    void deleteMaterial(Long id);

    /** 入库：创建批次+入库记录+更新库存（FR-0002） */
    StockInResultVO stockIn(StockInDTO dto, Long operatorId);
}
