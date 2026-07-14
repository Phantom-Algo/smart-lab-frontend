package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxMessageManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/inboxes/{userId}/messages")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInboxMessageController {

	private final InboxMessageManagementService inboxMessageManagementService;

	@GetMapping
	public ApiResult<PageResult<InboxMessageListItemResponse>> page(
			@PathVariable @Positive(message = "用户ID必须为正数") Long userId,
			@Valid @ModelAttribute InboxMessagePageQueryRequest request) {
		return ApiResult.success(inboxMessageManagementService.pageUserInbox(userId, request));
	}

	@GetMapping("/{inboxMessageId}")
	public ApiResult<InboxMessageDetailResponse> getDetail(
			@PathVariable @Positive(message = "用户ID必须为正数") Long userId,
			@PathVariable @Positive(message = "收件箱消息ID必须为正数") Long inboxMessageId) {
		return ApiResult.success(inboxMessageManagementService.getUserMessage(userId, inboxMessageId));
	}

	@DeleteMapping("/{inboxMessageId}")
	public ApiResult<Void> delete(
			@PathVariable @Positive(message = "用户ID必须为正数") Long userId,
			@PathVariable @Positive(message = "收件箱消息ID必须为正数") Long inboxMessageId) {
		inboxMessageManagementService.deleteUserMessage(userId, inboxMessageId);
		return ApiResult.success();
	}

	@DeleteMapping("/batch")
	public ApiResult<Void> deleteBatch(
			@PathVariable @Positive(message = "用户ID必须为正数") Long userId,
			@Valid @RequestBody BatchDeleteInboxMessageRequest request) {
		inboxMessageManagementService.deleteUserMessages(userId, request);
		return ApiResult.success();
	}
}
