package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabMapper extends BaseMapper<LabDO> {
}
