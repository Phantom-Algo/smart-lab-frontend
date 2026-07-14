package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.UpdateMyProfileRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.MyProfileResponse;

public interface ProfileService {

	MyProfileResponse getCurrentProfile();

	MyProfileResponse updateCurrentProfile(UpdateMyProfileRequest request);
}
