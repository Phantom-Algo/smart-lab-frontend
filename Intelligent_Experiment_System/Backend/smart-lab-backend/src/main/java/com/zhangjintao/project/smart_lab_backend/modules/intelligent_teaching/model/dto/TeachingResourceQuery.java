package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeachingResourceQuery {

	Long courseId;
	Integer resourceType;
	String keyword;
	Long uploadedBy;
	Long pageNum;
	Long pageSize;
}
