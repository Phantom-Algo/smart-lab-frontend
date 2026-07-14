package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubmissionStatsDTO {

	Long taskId;
	long expectedCount;
	long submittedCount;
	long unsubmittedCount;
	long returnedCount;
	long lateCount;
}
