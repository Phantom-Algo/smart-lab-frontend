package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageDeadLetter;

/**
 * 死信消息仓储接口
 */
public interface MsgDeadLetterRepository {

	/**
	 * 插入死信记录
	 *
	 * @param deadLetter 死信消息
	 */
	void insert(MessageDeadLetter deadLetter);
}
