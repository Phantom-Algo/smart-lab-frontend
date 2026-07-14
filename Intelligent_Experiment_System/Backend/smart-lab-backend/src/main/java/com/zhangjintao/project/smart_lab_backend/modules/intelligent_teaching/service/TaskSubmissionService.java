package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TaskSubmissionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionHistoryItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionStatsResponse;

import java.util.List;

public interface TaskSubmissionService {

	TaskSubmissionDetailResponse submit(Long taskId, TaskSubmissionCreateRequest request);

	TaskSubmissionDetailResponse getMyCurrentSubmission(Long taskId);

	List<TaskSubmissionHistoryItemResponse> listMyHistory(Long taskId);

	List<TaskSubmissionDetailResponse> listTaskCurrentSubmissions(Long taskId);

	List<TaskSubmissionHistoryItemResponse> listStudentHistory(Long taskId, Long studentId);

	TaskSubmissionStatsResponse getTaskStats(Long taskId);
}
