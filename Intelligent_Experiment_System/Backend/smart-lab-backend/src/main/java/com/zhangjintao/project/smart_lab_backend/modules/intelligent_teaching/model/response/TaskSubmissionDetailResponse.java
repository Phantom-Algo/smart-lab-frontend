package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskSubmissionDetailResponse {

	Long submissionId;
	String submissionNo;
	Long taskId;
	String taskTitle;
	Long studentId;
	String studentName;
	Integer versionNo;
	Integer currentFlag;
	Integer submitStatus;
	Integer isLate;
	String contentText;
	String attachments;
	String returnedReason;
	LocalDateTime returnedAt;
	LocalDateTime submittedAt;
}
