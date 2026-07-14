package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileDO> {
}
