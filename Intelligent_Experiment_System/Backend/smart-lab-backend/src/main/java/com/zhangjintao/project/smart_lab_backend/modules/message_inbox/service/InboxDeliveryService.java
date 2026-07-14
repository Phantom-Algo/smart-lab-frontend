package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import java.util.List;

/**
 * 收件箱投递服务接口
 */
public interface InboxDeliveryService {

	/**
	 * 投递消息到用户收件箱
	 *
	 * @param messageId       消息ID
	 * @param recipientUserIds 收件人用户ID列表
	 * @return 插入行数
	 */
	int deliver(Long messageId, List<Long> recipientUserIds);
}
