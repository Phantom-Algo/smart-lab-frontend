package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息模板分页查询请求。
 */
@Data
public class MessageTemplatePageQueryRequest {

	@Min(value = 1, message = "页码必须大于等于1")
	private Long pageNum = 1L;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 500, message = "每页条数不能超过500")
	private Long pageSize = 10L;

	@Size(max = 128, message = "模板编码长度不能超过128")
	private String templateCode;

	@Size(max = 128, message = "模板名称长度不能超过128")
	private String templateName;

	@Size(max = 128, message = "消息类型长度不能超过128")
	private String messageType;

	@Min(value = 0, message = "模板状态只能是0或1")
	@Max(value = 1, message = "模板状态只能是0或1")
	private Integer status;
}
