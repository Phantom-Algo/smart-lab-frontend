package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;

/**
 * 消息模板服务接口
 */
public interface MessageTemplateService {

	/**
	 * 解析消息模板
	 *
	 * @param command 消息发布命令
	 * @return 模板快照
	 */
	MessageTemplateSnapshot resolveTemplate(MessagePublishCommand command);
}
