package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.BatchGradeDraftRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeDraftUpsertRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseGradeStatsResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.GradeItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.StudentGradeSheetResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.GradeAssessmentService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.GradeItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GradeController {

	private final GradeItemService gradeItemService;
	private final GradeAssessmentService gradeAssessmentService;

	@PostMapping("/api/teaching/courses/{courseId}/grade-items")
	public ApiResult<GradeItemResponse> createGradeItem(@PathVariable Long courseId,
			@Valid @RequestBody GradeItemCreateRequest request) {
		return ApiResult.success(gradeItemService.create(courseId, request));
	}

	@PutMapping("/api/teaching/grade-items/{id}")
	public ApiResult<GradeItemResponse> updateGradeItem(@PathVariable Long id,
			@Valid @RequestBody GradeItemUpdateRequest request) {
		return ApiResult.success(gradeItemService.update(id, request));
	}

	@DeleteMapping("/api/teaching/grade-items/{id}")
	public ApiResult<Void> deleteGradeItem(@PathVariable Long id) {
		gradeItemService.delete(id);
		return ApiResult.success();
	}

	@GetMapping("/api/teaching/courses/{courseId}/grade-items")
	public ApiResult<List<GradeItemResponse>> listGradeItems(@PathVariable Long courseId) {
		return ApiResult.success(gradeItemService.listByCourseId(courseId));
	}

	@PostMapping("/api/teaching/courses/{courseId}/grade-items/reorder")
	public ApiResult<Void> reorderGradeItems(@PathVariable Long courseId,
			@Valid @RequestBody GradeItemReorderRequest request) {
		gradeItemService.reorder(courseId, request);
		return ApiResult.success();
	}

	@PostMapping("/api/teaching/courses/{courseId}/grades/draft")
	public ApiResult<Void> saveGradeDraft(@PathVariable Long courseId, @Valid @RequestBody GradeDraftUpsertRequest request) {
		gradeAssessmentService.saveDraft(courseId, request);
		return ApiResult.success();
	}

	@PostMapping("/api/teaching/courses/{courseId}/grades/draft/batch")
	public ApiResult<Void> batchSaveGradeDraft(@PathVariable Long courseId,
			@Valid @RequestBody BatchGradeDraftRequest request) {
		gradeAssessmentService.batchSaveDraft(courseId, request);
		return ApiResult.success();
	}

	@GetMapping("/api/teaching/courses/{courseId}/grades")
	public ApiResult<List<StudentGradeSheetResponse>> listCourseGradeSheets(@PathVariable Long courseId) {
		return ApiResult.success(gradeAssessmentService.listCourseGradeSheets(courseId));
	}

	@GetMapping("/api/teaching/courses/{courseId}/grades/stats")
	public ApiResult<CourseGradeStatsResponse> getCourseGradeStats(@PathVariable Long courseId) {
		return ApiResult.success(gradeAssessmentService.getCourseGradeStats(courseId));
	}

	@PostMapping("/api/teaching/courses/{courseId}/grades/publish")
	public ApiResult<Void> publishCourseGrades(@PathVariable Long courseId) {
		gradeAssessmentService.publishCourseGrades(courseId);
		return ApiResult.success();
	}

	@GetMapping("/api/teaching/courses/{courseId}/grades/me")
	public ApiResult<StudentGradeSheetResponse> getMyGradeSheet(@PathVariable Long courseId) {
		return ApiResult.success(gradeAssessmentService.getMyGradeSheet(courseId));
	}
}
