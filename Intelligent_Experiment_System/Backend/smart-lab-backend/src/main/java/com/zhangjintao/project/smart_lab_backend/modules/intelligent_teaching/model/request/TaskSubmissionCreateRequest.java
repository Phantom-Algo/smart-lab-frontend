package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import lombok.Data;

@Data
public class TaskSubmissionCreateRequest {

	private String contentText;
	private String attachments;
}
