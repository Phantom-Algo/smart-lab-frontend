package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.RealtimePublisher;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxMessageRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 收件箱投递服务实现
 */
@Service
@RequiredArgsConstructor
public class InboxDeliveryServiceImpl implements InboxDeliveryService {

	private final UserInboxMessageRepository userInboxMessageRepository;
	private final RealtimePublisher realtimePublisher;

	@Override
	public int deliver(Long messageId, List<Long> recipientUserIds) {
		LocalDateTime receivedAt = LocalDateTime.now();
		List<UserInboxMessage> inboxMessages = recipientUserIds.stream()
				.distinct()
				.map(userId -> UserInboxMessage.unread(messageId, userId, receivedAt))
				.toList();
		int affected = userInboxMessageRepository.insertIgnoreBatch(inboxMessages);
		if (affected > 0 && !recipientUserIds.isEmpty()) {
			realtimePublisher.publish(RealtimeEvent.builder()
					.event("message.new")
					.source("message_inbox")
					.targetType(RealtimeTargetType.USERS)
					.targetUserIds(recipientUserIds.stream().distinct().toList())
					.data(Map.of("messageId", messageId, "receivedAt", receivedAt, "unreadHint", true))
					.occurredAt(receivedAt)
					.build());
		}
		return affected;
	}
}
