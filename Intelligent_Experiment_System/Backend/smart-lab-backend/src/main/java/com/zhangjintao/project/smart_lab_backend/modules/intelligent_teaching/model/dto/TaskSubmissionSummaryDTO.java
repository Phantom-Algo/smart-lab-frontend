package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskSubmissionSummaryDTO {

	Long submissionId;
	String submissionNo;
	Long taskId;
	Long studentId;
	Integer versionNo;
	Integer submitStatus;
	Integer isLate;
	LocalDateTime submittedAt;
}
