package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.AlertQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.AlertRecordVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 设备告警管理服务接口
 */
public interface DeviceAlertService {
    /** FR-0017: 分页查询告警记录 */
    PageResult<AlertRecordVO> listAlerts(AlertQueryDTO query);

    /** FR-0017: 处理告警（仅未处理可处理） */
    void handleAlert(Long id, Long handlerId, String handleResult);
}
