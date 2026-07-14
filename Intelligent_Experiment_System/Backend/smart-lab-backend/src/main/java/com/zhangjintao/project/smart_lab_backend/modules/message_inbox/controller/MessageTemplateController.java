package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.CreateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.MessageTemplatePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.UpdateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.MessageTemplateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/message-templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MessageTemplateController {

	private final MessageTemplateManagementService messageTemplateManagementService;

	@PostMapping
	public ApiResult<MessageTemplateResponse> create(@Valid @RequestBody CreateMessageTemplateRequest request) {
		return ApiResult.success(messageTemplateManagementService.create(request));
	}

	@GetMapping("/{id}")
	public ApiResult<MessageTemplateResponse> getById(@PathVariable @Positive(message = "模板ID必须为正数") Long id) {
		return ApiResult.success(messageTemplateManagementService.getById(id));
	}

	@GetMapping
	public ApiResult<PageResult<MessageTemplateResponse>> page(@Valid @ModelAttribute MessageTemplatePageQueryRequest request) {
		return ApiResult.success(messageTemplateManagementService.page(request));
	}

	@PutMapping("/{id}")
	public ApiResult<MessageTemplateResponse> update(@PathVariable @Positive(message = "模板ID必须为正数") Long id,
			@Valid @RequestBody UpdateMessageTemplateRequest request) {
		return ApiResult.success(messageTemplateManagementService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable @Positive(message = "模板ID必须为正数") Long id) {
		messageTemplateManagementService.delete(id);
		return ApiResult.success();
	}
}
