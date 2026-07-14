package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;

import java.util.List;

/**
 * 收件人解析服务接口
 */
public interface RecipientResolveService {

	/**
	 * 解析消息收件人用户ID列表
	 *
	 * @param command 消息发布命令
	 * @return 收件人用户ID列表
	 */
	List<Long> resolveRecipients(MessagePublishCommand command);
}
