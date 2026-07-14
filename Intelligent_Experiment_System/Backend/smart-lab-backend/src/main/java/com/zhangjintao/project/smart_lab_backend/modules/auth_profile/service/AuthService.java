package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.ChangePasswordRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.LoginRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.LoginResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.SecurityOverviewResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

	LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest);

	void logoutCurrentSession();

	void changePassword(ChangePasswordRequest request);

	SecurityOverviewResponse getCurrentSecurityOverview();
}
