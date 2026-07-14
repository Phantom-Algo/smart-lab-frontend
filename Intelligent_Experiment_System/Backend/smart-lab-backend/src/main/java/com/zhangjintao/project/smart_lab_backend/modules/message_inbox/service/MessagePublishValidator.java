package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;

/**
 * 消息发布校验接口
 */
public interface MessagePublishValidator {

	/**
	 * 校验消息发布命令
	 *
	 * @param command 消息发布命令
	 */
	void validate(MessagePublishCommand command);
}
