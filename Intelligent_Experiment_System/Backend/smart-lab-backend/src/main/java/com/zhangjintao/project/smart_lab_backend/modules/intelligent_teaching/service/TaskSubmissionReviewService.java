package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.SubmissionReturnRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionDetailResponse;

public interface TaskSubmissionReviewService {

	TaskSubmissionDetailResponse returnSubmission(Long submissionId, SubmissionReturnRequest request);
}
