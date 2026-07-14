package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

/**
 * 消息分发工作器接口
 */
public interface MessageDispatchWorker {

	/**
	 * 执行一次消息分发
	 */
	void dispatchOnce();
}
