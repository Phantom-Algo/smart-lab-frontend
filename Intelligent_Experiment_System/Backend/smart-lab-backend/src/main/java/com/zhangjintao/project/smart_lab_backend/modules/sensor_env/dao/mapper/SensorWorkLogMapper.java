package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject.SensorWorkLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 传感器工作日志 Mapper 接口。
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Mapper
public interface SensorWorkLogMapper extends BaseMapper<SensorWorkLogDO> {
}
