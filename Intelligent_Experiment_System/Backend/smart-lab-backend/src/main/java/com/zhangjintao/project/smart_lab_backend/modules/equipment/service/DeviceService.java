package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 设备台账管理服务接口
 *
 * @author linbowei
 */
public interface DeviceService {

    /**
     * 分页查询设备列表
     */
    PageResult<DeviceVO> listDevices(DeviceQueryDTO query);

    /**
     * 查询设备详情
     */
    DeviceVO getDevice(Long id);

    /**
     * 创建设备（管理员）
     */
    DeviceVO createDevice(DeviceCreateDTO dto);

    /**
     * 更新设备信息（管理员）
     */
    DeviceVO updateDevice(Long id, DeviceUpdateDTO dto);

    /**
     * 删除设备（仅限报废状态，管理员）
     */
    void deleteDevice(Long id);
}
