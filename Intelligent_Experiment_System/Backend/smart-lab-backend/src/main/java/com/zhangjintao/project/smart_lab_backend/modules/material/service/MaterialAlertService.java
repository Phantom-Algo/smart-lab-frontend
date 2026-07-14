package com.zhangjintao.project.smart_lab_backend.modules.material.service;

import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialAlertVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 预警管理服务接口 — FR-0007, FR-0008
 *
 * @author linbowei
 */
public interface MaterialAlertService {

    /** 低库存检测（FR-0007 定时任务） */
    void checkLowStock();

    /** 临期/过期检测（FR-0007 定时任务） */
    void checkExpiry();

    /** 超量领用预警（FR-0007 审批时实时触发） */
    void createOverReceiveAlert(Long materialId, int quantity);

    /** 预警分页查询（FR-0008） */
    PageResult<MaterialAlertVO> pageAlerts(AlertQueryDTO query);

    /** 处理预警（FR-0008） */
    void handleAlert(Long id, Long handlerId, String result);
}
