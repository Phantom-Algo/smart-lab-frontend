package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskSubmissionHistoryItemResponse {

	Long submissionId;
	Integer versionNo;
	Integer currentFlag;
	Integer submitStatus;
	Integer isLate;
	String returnedReason;
	LocalDateTime submittedAt;
}
