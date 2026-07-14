package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileException;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserQueryServiceImpl implements CurrentUserQueryService {

	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	public AuthenticatedUser requireCurrentUser() {
		return SecurityContextUtils.getAuthenticatedUser()
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.SESSION_NOT_FOUND));
	}

	@Override
	public UserAccountWithProfileDTO requireCurrentUserWithProfile() {
		AuthenticatedUser currentUser = requireCurrentUser();
		UserAccountDO userAccountDO = userAccountRepository.findById(currentUser.getUserId())
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.SESSION_NOT_FOUND));
		UserProfileDO userProfileDO = userProfileRepository.findByUserId(currentUser.getUserId())
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.PROFILE_NOT_FOUND));
		return UserAccountWithProfileDTO.builder()
				.userAccount(userAccountDO)
				.userProfile(userProfileDO)
				.build();
	}
}
