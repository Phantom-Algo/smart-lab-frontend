package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.SubmissionReturnRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TaskSubmissionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionHistoryItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionStatsResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TaskSubmissionReviewService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TaskSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskSubmissionController {

	private final TaskSubmissionService taskSubmissionService;
	private final TaskSubmissionReviewService taskSubmissionReviewService;

	@PostMapping("/api/teaching/tasks/{taskId}/submissions")
	public ApiResult<TaskSubmissionDetailResponse> submit(@PathVariable Long taskId,
			@RequestBody(required = false) TaskSubmissionCreateRequest request) {
		return ApiResult.success(taskSubmissionService.submit(taskId,
				request == null ? new TaskSubmissionCreateRequest() : request));
	}

	@GetMapping("/api/teaching/tasks/{taskId}/submissions/me")
	public ApiResult<TaskSubmissionDetailResponse> getMyCurrentSubmission(@PathVariable Long taskId) {
		return ApiResult.success(taskSubmissionService.getMyCurrentSubmission(taskId));
	}

	@GetMapping("/api/teaching/tasks/{taskId}/submissions/me/history")
	public ApiResult<List<TaskSubmissionHistoryItemResponse>> listMyHistory(@PathVariable Long taskId) {
		return ApiResult.success(taskSubmissionService.listMyHistory(taskId));
	}

	@GetMapping("/api/teaching/tasks/{taskId}/submissions")
	public ApiResult<List<TaskSubmissionDetailResponse>> listTaskCurrentSubmissions(@PathVariable Long taskId) {
		return ApiResult.success(taskSubmissionService.listTaskCurrentSubmissions(taskId));
	}

	@GetMapping("/api/teaching/tasks/{taskId}/submissions/{studentId}/history")
	public ApiResult<List<TaskSubmissionHistoryItemResponse>> listStudentHistory(@PathVariable Long taskId,
			@PathVariable Long studentId) {
		return ApiResult.success(taskSubmissionService.listStudentHistory(taskId, studentId));
	}

	@GetMapping("/api/teaching/tasks/{taskId}/submission-stats")
	public ApiResult<TaskSubmissionStatsResponse> getTaskStats(@PathVariable Long taskId) {
		return ApiResult.success(taskSubmissionService.getTaskStats(taskId));
	}

	@PostMapping("/api/teaching/submissions/{submissionId}/return")
	public ApiResult<TaskSubmissionDetailResponse> returnSubmission(@PathVariable Long submissionId,
			@Valid @RequestBody SubmissionReturnRequest request) {
		return ApiResult.success(taskSubmissionReviewService.returnSubmission(submissionId, request));
	}
}
