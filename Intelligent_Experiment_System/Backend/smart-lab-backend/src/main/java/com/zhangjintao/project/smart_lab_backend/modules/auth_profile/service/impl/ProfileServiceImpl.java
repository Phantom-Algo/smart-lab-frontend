package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileException;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.converter.AuthProfileConverter;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.UpdateMyProfileRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.MyProfileResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

	private final CurrentUserQueryService currentUserQueryService;
	private final UserProfileRepository userProfileRepository;

	@Override
	public MyProfileResponse getCurrentProfile() {
		UserAccountWithProfileDTO currentUser = currentUserQueryService.requireCurrentUserWithProfile();
		return AuthProfileConverter.toMyProfileResponse(currentUser.getUserAccount(), currentUser.getUserProfile());
	}

	@Override
	@Transactional
	public MyProfileResponse updateCurrentProfile(UpdateMyProfileRequest request) {
		UserAccountWithProfileDTO currentUser = currentUserQueryService.requireCurrentUserWithProfile();
		UserProfileDO userProfileDO = currentUser.getUserProfile();
		if (userProfileRepository.existsByPhone(request.getPhone(), currentUser.getUserAccount().getId())) {
			throw new AuthProfileException(AuthProfileErrorCode.PHONE_ALREADY_EXISTS);
		}
		if (userProfileRepository.existsByEmail(request.getEmail(), currentUser.getUserAccount().getId())) {
			throw new AuthProfileException(AuthProfileErrorCode.EMAIL_ALREADY_EXISTS);
		}
		userProfileDO.setPhone(request.getPhone());
		userProfileDO.setEmail(request.getEmail());
		userProfileDO.setAvatarUrl(request.getAvatarUrl());
		userProfileRepository.update(userProfileDO);
		return AuthProfileConverter.toMyProfileResponse(currentUser.getUserAccount(), userProfileDO);
	}
}
