package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MsgEventMapper extends BaseMapper<MsgEventDO> {
}
