package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeachingTaskQuery {

	Long courseId;
	Long createdBy;
	String keyword;
	Integer taskStatus;
	Long pageNum;
	Long pageSize;
}
