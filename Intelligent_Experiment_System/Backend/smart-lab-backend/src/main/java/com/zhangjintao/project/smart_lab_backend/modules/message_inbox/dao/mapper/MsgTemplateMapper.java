package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MsgTemplateMapper extends BaseMapper<MsgTemplateDO> {
}
