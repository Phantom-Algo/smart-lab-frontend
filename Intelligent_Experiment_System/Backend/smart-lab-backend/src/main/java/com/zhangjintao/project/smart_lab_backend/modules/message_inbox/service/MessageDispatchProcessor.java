package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageDispatchResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;

/**
 * 消息分发处理服务接口
 */
public interface MessageDispatchProcessor {

	/**
	 * 处理消息发布命令
	 *
	 * @param command 消息发布命令
	 * @return 分发结果
	 */
	MessageDispatchResult process(MessagePublishCommand command);

	/**
	 * 标记事件分发失败
	 *
	 * @param eventNo 事件编号
	 */
	void markFailed(String eventNo);
}
