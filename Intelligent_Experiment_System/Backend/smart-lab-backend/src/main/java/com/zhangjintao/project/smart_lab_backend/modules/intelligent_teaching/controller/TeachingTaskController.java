package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskPublishRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskPageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeachingTaskController {

	private final TeachingTaskService teachingTaskService;

	@PostMapping("/api/teaching/tasks")
	public ApiResult<TeachingTaskDetailResponse> create(@Valid @RequestBody TeachingTaskCreateRequest request) {
		return ApiResult.success(teachingTaskService.create(request));
	}

	@PutMapping("/api/teaching/tasks/{id}")
	public ApiResult<TeachingTaskDetailResponse> update(@PathVariable Long id,
			@Valid @RequestBody TeachingTaskUpdateRequest request) {
		return ApiResult.success(teachingTaskService.update(id, request));
	}

	@GetMapping("/api/teaching/tasks/{id}")
	public ApiResult<TeachingTaskDetailResponse> getById(@PathVariable Long id) {
		return ApiResult.success(teachingTaskService.getById(id));
	}

	@GetMapping("/api/teaching/tasks")
	public ApiResult<PageResult<TeachingTaskPageItemResponse>> page(TeachingTaskQueryRequest request) {
		return ApiResult.success(teachingTaskService.page(request));
	}

	@GetMapping("/api/teaching/courses/{courseId}/tasks")
	public ApiResult<List<TeachingTaskPageItemResponse>> listByCourseId(@PathVariable Long courseId) {
		return ApiResult.success(teachingTaskService.listByCourseId(courseId));
	}

	@PostMapping("/api/teaching/tasks/{id}/publish")
	public ApiResult<TeachingTaskDetailResponse> publish(@PathVariable Long id,
			@RequestBody(required = false) TeachingTaskPublishRequest request) {
		return ApiResult.success(teachingTaskService.publish(id, request == null ? new TeachingTaskPublishRequest() : request));
	}

	@PostMapping("/api/teaching/tasks/{id}/close")
	public ApiResult<Void> close(@PathVariable Long id) {
		teachingTaskService.close(id);
		return ApiResult.success();
	}
}
