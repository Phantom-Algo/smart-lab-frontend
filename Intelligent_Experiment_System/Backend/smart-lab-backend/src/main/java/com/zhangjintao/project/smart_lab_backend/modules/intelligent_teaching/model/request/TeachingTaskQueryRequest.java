package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import lombok.Data;

@Data
public class TeachingTaskQueryRequest {

	private Long courseId;
	private Long createdBy;
	private String keyword;
	private Integer taskStatus;
	private Long pageNum = 1L;
	private Long pageSize = 10L;
}
