package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TeachingTaskPageItemResponse {

	Long taskId;
	String taskNo;
	Long courseId;
	String courseName;
	String taskTitle;
	Integer taskStatus;
	LocalDateTime deadline;
	LocalDateTime publishedAt;
	String createdByName;
}
