package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagePublishResult {

	private String eventNo;

	private String streamRecordId;
}
