package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TeachingTaskSummaryDTO {

	Long taskId;
	String taskNo;
	Long courseId;
	String taskTitle;
	Integer taskStatus;
	LocalDateTime deadline;
	LocalDateTime publishedAt;
}
