package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBatchEnrollRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBindLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseLabBindingResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CoursePageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseLabService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teaching/courses")
@RequiredArgsConstructor
public class CourseController {

	private final CourseService courseService;
	private final CourseLabService courseLabService;

	@PostMapping
	public ApiResult<CourseDetailResponse> create(@Valid @RequestBody CourseCreateRequest request) {
		return ApiResult.success(courseService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResult<CourseDetailResponse> update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
		return ApiResult.success(courseService.update(id, request));
	}

	@GetMapping("/{id}")
	public ApiResult<CourseDetailResponse> getById(@PathVariable Long id) {
		return ApiResult.success(courseService.getById(id));
	}

	@GetMapping
	public ApiResult<PageResult<CoursePageItemResponse>> page(CourseQueryRequest request) {
		return ApiResult.success(courseService.page(request));
	}

	@PostMapping("/{id}/archive")
	public ApiResult<Void> archive(@PathVariable Long id) {
		courseService.archive(id);
		return ApiResult.success();
	}

	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		courseService.delete(id);
		return ApiResult.success();
	}

	@GetMapping("/{id}/students")
	public ApiResult<List<CourseStudentResponse>> listStudents(@PathVariable Long id) {
		return ApiResult.success(courseService.listStudents(id));
	}

	@PostMapping("/{id}/students:batch-enroll")
	public ApiResult<Void> batchEnroll(@PathVariable Long id, @Valid @RequestBody CourseBatchEnrollRequest request) {
		courseService.batchEnroll(id, request);
		return ApiResult.success();
	}

	@PostMapping("/{id}/students:batch-withdraw")
	public ApiResult<Void> batchWithdraw(@PathVariable Long id, @Valid @RequestBody CourseBatchEnrollRequest request) {
		courseService.batchWithdraw(id, request);
		return ApiResult.success();
	}

	@GetMapping("/{id}/labs")
	public ApiResult<List<CourseLabBindingResponse>> listLabs(@PathVariable Long id) {
		return ApiResult.success(courseLabService.listByCourseId(id));
	}

	@PostMapping("/{id}/labs")
	public ApiResult<CourseLabBindingResponse> bindLab(@PathVariable Long id, @Valid @RequestBody CourseBindLabRequest request) {
		return ApiResult.success(courseLabService.bind(id, request));
	}

	@PostMapping("/{id}/labs/{labId}/unbind")
	public ApiResult<Void> unbindLab(@PathVariable Long id, @PathVariable Long labId) {
		courseLabService.unbind(id, labId);
		return ApiResult.success();
	}
}
