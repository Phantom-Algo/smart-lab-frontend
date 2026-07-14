package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户收件箱消息仓储接口
 */
public interface UserInboxMessageRepository {

	/**
	 * 批量插入用户收件箱消息（忽略重复）
	 *
	 * @param inboxMessages 收件箱消息列表
	 * @return 插入行数
	 */
	int insertIgnoreBatch(List<UserInboxMessage> inboxMessages);

	PageResult<InboxMessageQueryDTO> page(Long userId, long pageNum, long pageSize, MessagePriority priority,
			String messageType, Boolean read);

	List<Long> listNewInboxMessageIds(Long userId, LocalDateTime lastViewedAt);

	Optional<InboxMessageQueryDTO> findActiveDetail(Long userId, Long inboxMessageId);

	long countActiveByUserIdAndIds(Long userId, List<Long> inboxMessageIds);

	int markRead(Long userId, Long inboxMessageId, LocalDateTime readAt);

	int softDelete(Long userId, Long inboxMessageId, LocalDateTime deletedAt);

	int softDeleteBatch(Long userId, List<Long> inboxMessageIds, LocalDateTime deletedAt);
}
