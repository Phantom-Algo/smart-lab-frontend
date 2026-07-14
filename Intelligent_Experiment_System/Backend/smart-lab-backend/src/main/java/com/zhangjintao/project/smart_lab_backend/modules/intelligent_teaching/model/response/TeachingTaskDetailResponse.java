package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class TeachingTaskDetailResponse {

	Long taskId;
	String taskNo;
	Long courseId;
	String courseName;
	String taskTitle;
	String taskContent;
	Integer submitType;
	Integer maxFileSize;
	LocalDateTime deadline;
	BigDecimal taskWeight;
	Integer taskStatus;
	LocalDateTime publishedAt;
	Long createdBy;
	String createdByName;
}
