package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.value.MessageContentSnapshot;

import java.util.Optional;

/**
 * 消息事件仓储接口
 */
public interface MsgEventRepository {

	/**
	 * 根据事件编号查询消息事件
	 *
	 * @param eventNo 事件编号
	 * @return 消息事件
	 */
	Optional<MessageEvent> findByEventNo(String eventNo);

	/**
	 * 插入消息事件
	 *
	 * @param messageEvent 消息事件
	 * @return 带数据库主键的消息事件
	 */
	MessageEvent insert(MessageEvent messageEvent);

	/**
	 * 标记事件为处理中
	 *
	 * @param id 事件ID
	 */
	void markProcessing(Long id);

	/**
	 * 标记事件为已完成
	 *
	 * @param id              事件ID
	 * @param contentSnapshot 消息内容快照
	 */
	void markCompleted(Long id, MessageContentSnapshot contentSnapshot);

	/**
	 * 标记事件为失败
	 *
	 * @param id 事件ID
	 */
	void markFailed(Long id);
}
