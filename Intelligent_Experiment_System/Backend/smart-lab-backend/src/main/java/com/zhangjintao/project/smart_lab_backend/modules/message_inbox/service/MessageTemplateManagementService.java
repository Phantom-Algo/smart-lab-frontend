package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.CreateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.MessageTemplatePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.UpdateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.MessageTemplateResponse;

/**
 * 消息模板管理服务。
 */
public interface MessageTemplateManagementService {

	MessageTemplateResponse create(CreateMessageTemplateRequest request);

	MessageTemplateResponse getById(Long id);

	PageResult<MessageTemplateResponse> page(MessageTemplatePageQueryRequest request);

	MessageTemplateResponse update(Long id, UpdateMessageTemplateRequest request);

	void delete(Long id);
}
