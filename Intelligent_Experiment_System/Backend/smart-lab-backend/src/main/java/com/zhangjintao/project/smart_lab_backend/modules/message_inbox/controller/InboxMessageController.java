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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/inbox/messages")
@RequiredArgsConstructor
public class InboxMessageController {

	private final InboxMessageManagementService inboxMessageManagementService;

	@GetMapping
	public ApiResult<PageResult<InboxMessageListItemResponse>> page(@Valid @ModelAttribute InboxMessagePageQueryRequest request) {
		return ApiResult.success(inboxMessageManagementService.pageMyInbox(request));
	}

	@GetMapping("/new-ids")
	public ApiResult<List<Long>> listNewIds() {
		return ApiResult.success(inboxMessageManagementService.listMyNewInboxMessageIds());
	}

	@PostMapping("/viewed")
	public ApiResult<Void> recordViewed() {
		inboxMessageManagementService.recordMyInboxViewed();
		return ApiResult.success();
	}

	@GetMapping("/{inboxMessageId}")
	public ApiResult<InboxMessageDetailResponse> getDetail(
			@PathVariable @Positive(message = "收件箱消息ID必须为正数") Long inboxMessageId) {
		return ApiResult.success(inboxMessageManagementService.getMyMessage(inboxMessageId));
	}

	@DeleteMapping("/{inboxMessageId}")
	public ApiResult<Void> delete(
			@PathVariable @Positive(message = "收件箱消息ID必须为正数") Long inboxMessageId) {
		inboxMessageManagementService.deleteMyMessage(inboxMessageId);
		return ApiResult.success();
	}

	@DeleteMapping("/batch")
	public ApiResult<Void> deleteBatch(@Valid @RequestBody BatchDeleteInboxMessageRequest request) {
		inboxMessageManagementService.deleteMyMessages(request);
		return ApiResult.success();
	}
}
