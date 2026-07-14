package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;

/**
 * 死信服务接口
 */
public interface DeadLetterService {

	/**
	 * 保存死信记录
	 *
	 * @param command      消息发布命令
	 * @param failureStage 失败阶段
	 * @param throwable    异常
	 * @param retryable    是否可重试
	 */
	void save(MessagePublishCommand command, FailureStage failureStage, Throwable throwable, boolean retryable);

	/**
	 * 保存原始负载死信记录
	 *
	 * @param rawPayload   原始负载
	 * @param failureStage 失败阶段
	 * @param throwable    异常
	 */
	void saveRawPayload(String rawPayload, FailureStage failureStage, Throwable throwable);
}
