package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.SensorReportDTO;

/**
 * 传感器数据接收服务接口
 * <p>
 * 物联网环境子系统通过此接口向设备子系统推送监测数据。
 * </p>
 *
 * @author linbowei
 */
public interface DeviceSensorService {

    /** FR-0002: 接收传感器上报数据，进行安全范围比对 */
    void receiveSensorData(SensorReportDTO report);
}
