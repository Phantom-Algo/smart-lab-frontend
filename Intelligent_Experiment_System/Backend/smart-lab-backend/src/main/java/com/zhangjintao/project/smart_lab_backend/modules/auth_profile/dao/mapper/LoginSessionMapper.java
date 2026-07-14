package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginSessionMapper extends BaseMapper<LoginSessionDO> {
}
