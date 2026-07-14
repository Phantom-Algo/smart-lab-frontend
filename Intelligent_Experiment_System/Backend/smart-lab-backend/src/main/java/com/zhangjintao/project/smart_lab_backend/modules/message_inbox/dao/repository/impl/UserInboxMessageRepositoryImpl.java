package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.UserInboxMessageMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxMessageRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter.MessageInboxConverter;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户收件箱消息仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserInboxMessageRepositoryImpl implements UserInboxMessageRepository {

	private final UserInboxMessageMapper userInboxMessageMapper;

	@Override
	public int insertIgnoreBatch(List<UserInboxMessage> inboxMessages) {
		if (inboxMessages == null || inboxMessages.isEmpty()) {
			return 0;
		}
		return userInboxMessageMapper.insertIgnoreBatch(inboxMessages.stream()
				.map(MessageInboxConverter::toUserInboxMessageDO)
				.toList());
	}

	@Override
	public PageResult<InboxMessageQueryDTO> page(Long userId, long pageNum, long pageSize, MessagePriority priority,
			String messageType, Boolean read) {
		Page<InboxMessageQueryDTO> page = userInboxMessageMapper.selectInboxMessagePage(
				new Page<>(pageNum, pageSize),
				userId,
				priority == null ? null : priority.getCode(),
				messageType,
				read == null ? null : (read ? 1 : 0));
		return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public List<Long> listNewInboxMessageIds(Long userId, LocalDateTime lastViewedAt) {
		return userInboxMessageMapper.selectNewInboxMessageIds(userId, lastViewedAt);
	}

	@Override
	public Optional<InboxMessageQueryDTO> findActiveDetail(Long userId, Long inboxMessageId) {
		return Optional.ofNullable(userInboxMessageMapper.selectActiveDetail(userId, inboxMessageId));
	}

	@Override
	public long countActiveByUserIdAndIds(Long userId, List<Long> inboxMessageIds) {
		if (inboxMessageIds == null || inboxMessageIds.isEmpty()) {
			return 0;
		}
		return userInboxMessageMapper.countActiveByUserIdAndIds(userId, inboxMessageIds);
	}

	@Override
	public int markRead(Long userId, Long inboxMessageId, LocalDateTime readAt) {
		return userInboxMessageMapper.markRead(userId, inboxMessageId, readAt);
	}

	@Override
	public int softDelete(Long userId, Long inboxMessageId, LocalDateTime deletedAt) {
		return userInboxMessageMapper.softDelete(userId, inboxMessageId, deletedAt);
	}

	@Override
	public int softDeleteBatch(Long userId, List<Long> inboxMessageIds, LocalDateTime deletedAt) {
		if (inboxMessageIds == null || inboxMessageIds.isEmpty()) {
			return 0;
		}
		return userInboxMessageMapper.softDeleteBatch(userId, inboxMessageIds, deletedAt);
	}
}
