package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import lombok.Data;

@Data
public class TeachingResourceQueryRequest {

	private Long courseId;
	private Integer resourceType;
	private String keyword;
	private Long uploadedBy;
	private Long pageNum = 1L;
	private Long pageSize = 10L;
}
