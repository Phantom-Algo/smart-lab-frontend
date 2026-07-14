package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * DeviceDO 对应的 Mapper 接口
 *
 * @author linbowei
 */
@Mapper
public interface DeviceMapper extends BaseMapper<DeviceDO> {
}
