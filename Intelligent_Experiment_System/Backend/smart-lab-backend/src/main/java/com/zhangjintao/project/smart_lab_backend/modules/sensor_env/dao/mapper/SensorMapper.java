package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SensorMapper extends BaseMapper<SensorDO> {
}
