package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentDO> {
}
