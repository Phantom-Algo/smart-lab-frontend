package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收件箱消息分页查询请求。
 */
@Data
public class InboxMessagePageQueryRequest {

	@Min(value = 1, message = "页码必须大于等于1")
	private Long pageNum = 1L;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 500, message = "每页条数不能超过500")
	private Long pageSize = 10L;

	@Min(value = 1, message = "消息优先级只能是1、2或3")
	@Max(value = 3, message = "消息优先级只能是1、2或3")
	private Integer priority;

	@Size(max = 128, message = "消息类型长度不能超过128")
	private String messageType;

	@Min(value = 0, message = "是否已读只能是0或1")
	@Max(value = 1, message = "是否已读只能是0或1")
	private Integer isRead;
}
