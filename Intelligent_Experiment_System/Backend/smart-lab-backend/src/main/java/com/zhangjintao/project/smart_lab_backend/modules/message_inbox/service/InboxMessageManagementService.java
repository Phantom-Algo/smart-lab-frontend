package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;

import java.util.List;

/**
 * 收件箱消息管理服务。
 */
public interface InboxMessageManagementService {

	PageResult<InboxMessageListItemResponse> pageMyInbox(InboxMessagePageQueryRequest request);

	List<Long> listMyNewInboxMessageIds();

	void recordMyInboxViewed();

	InboxMessageDetailResponse getMyMessage(Long inboxMessageId);

	void deleteMyMessage(Long inboxMessageId);

	void deleteMyMessages(BatchDeleteInboxMessageRequest request);

	PageResult<InboxMessageListItemResponse> pageUserInbox(Long userId, InboxMessagePageQueryRequest request);

	InboxMessageDetailResponse getUserMessage(Long userId, Long inboxMessageId);

	void deleteUserMessage(Long userId, Long inboxMessageId);

	void deleteUserMessages(Long userId, BatchDeleteInboxMessageRequest request);
}
