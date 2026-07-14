package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.UpdateMyProfileRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.MyProfileResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.ResourcePermissionResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.ProfileService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.ResourcePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;
	private final ResourcePermissionService resourcePermissionService;

	@GetMapping("/me")
	public ApiResult<MyProfileResponse> getCurrentProfile() {
		return ApiResult.success(profileService.getCurrentProfile());
	}

	@PutMapping("/me")
	public ApiResult<MyProfileResponse> updateCurrentProfile(@Valid @RequestBody UpdateMyProfileRequest request) {
		return ApiResult.success(profileService.updateCurrentProfile(request));
	}

	@GetMapping("/me/resource-permissions")
	public ApiResult<List<ResourcePermissionResponse>> listCurrentUserResourcePermissions(
			@RequestParam(required = false) Integer resourceType) {
		return ApiResult.success(resourcePermissionService.listCurrentUserPermissions(resourceType));
	}
}
