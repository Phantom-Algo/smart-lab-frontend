package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.RenderedMessage;

import java.util.Map;

/**
 * 模板渲染服务接口
 */
public interface TemplateRenderService {

	/**
	 * 渲染消息模板
	 *
	 * @param template 模板快照
	 * @param params   模板参数
	 * @return 渲染后的消息
	 */
	RenderedMessage render(MessageTemplateSnapshot template, Map<String, Object> params);
}
