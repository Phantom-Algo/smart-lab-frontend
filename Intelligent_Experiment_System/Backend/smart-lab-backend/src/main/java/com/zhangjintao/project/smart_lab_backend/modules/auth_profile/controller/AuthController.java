package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.ChangePasswordRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.LoginRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.LoginResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.SecurityOverviewResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
		return ApiResult.success(authService.login(request, httpServletRequest));
	}

	@PostMapping("/logout")
	public ApiResult<Void> logout() {
		authService.logoutCurrentSession();
		return ApiResult.success();
	}

	@PostMapping("/change-password")
	public ApiResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ApiResult.success();
	}

	@GetMapping("/security")
	public ApiResult<SecurityOverviewResponse> getSecurityOverview() {
		return ApiResult.success(authService.getCurrentSecurityOverview());
	}
}
