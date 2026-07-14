package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.ResourceUploadPrepareRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.ResourceUploadPrepareResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingResourceResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teaching/resources")
@RequiredArgsConstructor
public class TeachingResourceController {

	private final TeachingResourceService teachingResourceService;

	@PostMapping
	public ApiResult<TeachingResourceResponse> create(@Valid @RequestBody TeachingResourceCreateRequest request) {
		return ApiResult.success(teachingResourceService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResult<TeachingResourceResponse> update(@PathVariable Long id,
			@Valid @RequestBody TeachingResourceUpdateRequest request) {
		return ApiResult.success(teachingResourceService.update(id, request));
	}

	@GetMapping("/{id}")
	public ApiResult<TeachingResourceResponse> getById(@PathVariable Long id) {
		return ApiResult.success(teachingResourceService.getById(id));
	}

	@GetMapping
	public ApiResult<PageResult<TeachingResourceResponse>> page(TeachingResourceQueryRequest request) {
		return ApiResult.success(teachingResourceService.page(request));
	}

	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		teachingResourceService.delete(id);
		return ApiResult.success();
	}

	@PostMapping("/upload-token")
	public ApiResult<ResourceUploadPrepareResponse> prepareUpload(@Valid @RequestBody ResourceUploadPrepareRequest request) {
		return ApiResult.success(teachingResourceService.prepareUpload(request));
	}

	@PostMapping("/reorder")
	public ApiResult<Void> reorder(@Valid @RequestBody TeachingResourceReorderRequest request) {
		teachingResourceService.reorder(request);
		return ApiResult.success();
	}
}
