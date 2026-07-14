package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除收件箱消息请求。
 */
@Data
public class BatchDeleteInboxMessageRequest {

	@NotEmpty(message = "收件箱消息ID不能为空")
	@Size(max = 500, message = "单次最多删除500条消息")
	@JsonAlias("inbox_message_ids")
	private List<@NotNull(message = "收件箱消息ID不能为空") @Positive(message = "收件箱消息ID必须为正数") Long> inboxMessageIds;
}
