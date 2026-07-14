package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.RepairReportDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.RepairVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

import java.math.BigDecimal;

/**
 * 设备维修管理服务接口
 *
 * @author linbowei
 */
public interface DeviceRepairService {

    /** FR-0010: 故障报修 */
    RepairVO reportFault(RepairReportDTO dto);

    /** FR-0011: 开始维修 */
    void startRepair(String repairNo, Long handlerId);

    /** FR-0011: 维修完成 */
    RepairVO completeRepair(String repairNo, String repairDesc);

    /** FR-0011: 维修验收 */
    void acceptRepair(String repairNo);

    /** FR-0011: 费用登记 */
    void recordCost(String repairNo, BigDecimal cost, String remark);

    /** FR-0012: 维修记录查询 */
    PageResult<RepairVO> listRepairRecords(RepairQueryDTO query);
}
