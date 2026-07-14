package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;

public interface CurrentUserQueryService {

	AuthenticatedUser requireCurrentUser();

	UserAccountWithProfileDTO requireCurrentUserWithProfile();
}
