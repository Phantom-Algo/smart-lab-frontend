package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxMessageRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxVisitStatRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxMessageManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收件箱消息管理服务实现。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboxMessageManagementServiceImpl implements InboxMessageManagementService {

	private final UserInboxMessageRepository userInboxMessageRepository;
	private final UserInboxVisitStatRepository userInboxVisitStatRepository;
	private final CurrentUserQueryService currentUserQueryService;

	@Override
	public PageResult<InboxMessageListItemResponse> pageMyInbox(InboxMessagePageQueryRequest request) {
		return pageForUser(currentUserId(), request);
	}

	@Override
	public List<Long> listMyNewInboxMessageIds() {
		Long userId = currentUserId();
		LocalDateTime lastViewedAt = userInboxVisitStatRepository.findLastViewedAt(userId).orElse(null);
		return userInboxMessageRepository.listNewInboxMessageIds(userId, lastViewedAt);
	}

	@Override
	@Transactional
	public void recordMyInboxViewed() {
		userInboxVisitStatRepository.upsertLastViewedAt(currentUserId(), LocalDateTime.now());
	}

	@Override
	@Transactional
	public InboxMessageDetailResponse getMyMessage(Long inboxMessageId) {
		Long userId = currentUserId();
		InboxMessageQueryDTO inboxMessage = getDetailOrThrow(userId, inboxMessageId);
		if (!inboxMessage.isRead()) {
			userInboxMessageRepository.markRead(userId, inboxMessageId, LocalDateTime.now());
			inboxMessage = getDetailOrThrow(userId, inboxMessageId);
		}
		return InboxMessageDetailResponse.from(inboxMessage);
	}

	@Override
	@Transactional
	public void deleteMyMessage(Long inboxMessageId) {
		deleteUserMessage(currentUserId(), inboxMessageId);
	}

	@Override
	@Transactional
	public void deleteMyMessages(BatchDeleteInboxMessageRequest request) {
		deleteUserMessages(currentUserId(), request);
	}

	@Override
	public PageResult<InboxMessageListItemResponse> pageUserInbox(Long userId, InboxMessagePageQueryRequest request) {
		return pageForUser(userId, request);
	}

	@Override
	public InboxMessageDetailResponse getUserMessage(Long userId, Long inboxMessageId) {
		return InboxMessageDetailResponse.from(getDetailOrThrow(userId, inboxMessageId));
	}

	@Override
	@Transactional
	public void deleteUserMessage(Long userId, Long inboxMessageId) {
		int affectedRows = userInboxMessageRepository.softDelete(userId, inboxMessageId, LocalDateTime.now());
		if (affectedRows != 1) {
			throwInboxMessageNotFound(userId, inboxMessageId);
		}
	}

	@Override
	@Transactional
	public void deleteUserMessages(Long userId, BatchDeleteInboxMessageRequest request) {
		List<Long> inboxMessageIds = request.getInboxMessageIds().stream()
				.distinct()
				.toList();
		long activeCount = userInboxMessageRepository.countActiveByUserIdAndIds(userId, inboxMessageIds);
		if (activeCount != inboxMessageIds.size()) {
			throw new MessageInboxException(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND,
					"存在不属于该用户或已删除的收件箱消息，userId=" + userId);
		}
		int affectedRows = userInboxMessageRepository.softDeleteBatch(userId, inboxMessageIds, LocalDateTime.now());
		if (affectedRows != inboxMessageIds.size()) {
			throw new MessageInboxException(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND,
					"收件箱消息删除失败，可能已被删除，userId=" + userId);
		}
	}

	private PageResult<InboxMessageListItemResponse> pageForUser(Long userId, InboxMessagePageQueryRequest request) {
		PageResult<InboxMessageQueryDTO> pageResult = userInboxMessageRepository.page(userId, request.getPageNum(),
				request.getPageSize(), resolvePriority(request.getPriority()), resolveMessageType(request.getMessageType()),
				resolveRead(request.getIsRead()));
		return PageResult.of(pageResult.getRecords().stream()
				.map(InboxMessageListItemResponse::from)
				.toList(), pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
	}

	private InboxMessageQueryDTO getDetailOrThrow(Long userId, Long inboxMessageId) {
		return userInboxMessageRepository.findActiveDetail(userId, inboxMessageId)
				.orElseThrow(() -> new MessageInboxException(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND,
						"收件箱消息不存在，userId=" + userId + ", inboxMessageId=" + inboxMessageId));
	}

	private MessagePriority resolvePriority(Integer priority) {
		if (priority == null) {
			return null;
		}
		try {
			return MessagePriority.fromCode(priority);
		}
		catch (IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "消息优先级只能是1、2或3");
		}
	}

	private String resolveMessageType(String messageType) {
		return StringUtils.hasText(messageType) ? messageType : null;
	}

	private Boolean resolveRead(Integer isRead) {
		if (isRead == null) {
			return null;
		}
		if (isRead != 0 && isRead != 1) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "是否已读只能是0或1");
		}
		return isRead == 1;
	}

	private Long currentUserId() {
		AuthenticatedUser currentUser = currentUserQueryService.requireCurrentUser();
		return currentUser.getUserId();
	}

	private void throwInboxMessageNotFound(Long userId, Long inboxMessageId) {
		throw new MessageInboxException(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND,
				"收件箱消息不存在，userId=" + userId + ", inboxMessageId=" + inboxMessageId);
	}
}
