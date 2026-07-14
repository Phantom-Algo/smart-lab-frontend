package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishResult;
import lombok.Builder;
import lombok.Data;

/**
 * 管理员发布消息响应。
 */
@Data
@Builder
public class PublishMessageResponse {

	private String eventNo;

	private String streamRecordId;

	public static PublishMessageResponse from(MessagePublishResult result) {
		return PublishMessageResponse.builder()
				.eventNo(result.getEventNo())
				.streamRecordId(result.getStreamRecordId())
				.build();
	}
}
