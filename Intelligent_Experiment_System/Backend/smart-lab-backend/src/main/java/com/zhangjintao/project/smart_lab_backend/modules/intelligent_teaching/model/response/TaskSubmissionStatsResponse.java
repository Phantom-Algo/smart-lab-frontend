package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskSubmissionStatsResponse {

	Long taskId;
	long expectedCount;
	long submittedCount;
	long unsubmittedCount;
	long returnedCount;
	long lateCount;
}
