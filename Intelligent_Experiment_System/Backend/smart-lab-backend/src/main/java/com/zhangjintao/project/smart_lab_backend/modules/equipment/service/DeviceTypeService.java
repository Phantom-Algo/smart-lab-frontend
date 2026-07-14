package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceTypeCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.DeviceTypeUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceTypeVO;

import java.util.List;

/**
 * 设备类型管理服务接口
 *
 * @author linbowei
 */
public interface DeviceTypeService {

    /**
     * 查询所有设备类型
     */
    List<DeviceTypeVO> listTypes();

    /**
     * 查询设备类型详情
     */
    DeviceTypeVO getType(Long id);

    /**
     * 创建设备类型（管理员）
     */
    DeviceTypeVO createType(DeviceTypeCreateDTO dto);

    /**
     * 更新设备类型（管理员）
     */
    DeviceTypeVO updateType(Long id, DeviceTypeUpdateDTO dto);

    /**
     * 删除设备类型（管理员，需无设备引用）
     */
    void deleteType(Long id);
}
