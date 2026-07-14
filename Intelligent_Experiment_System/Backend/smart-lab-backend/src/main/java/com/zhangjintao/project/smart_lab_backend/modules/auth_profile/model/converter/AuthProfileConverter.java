package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.ResourcePermissionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.ResourceType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.LoginSessionItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.MyProfileResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.ResourcePermissionResponse;

public final class AuthProfileConverter {

	private AuthProfileConverter() {
	}

	public static MyProfileResponse toMyProfileResponse(UserAccountDO userAccount, UserProfileDO userProfile) {
		UserType userType = UserType.fromCode(userAccount.getUserType());
		return MyProfileResponse.builder()
				.userId(userAccount.getId())
				.profileId(userProfile.getId())
				.personId(userAccount.getPersonId())
				.username(userAccount.getUsername())
				.userType(userType.getCode())
				.userTypeName(userType.getDescription())
				.realName(userProfile.getRealName())
				.genderType(userProfile.getGenderType())
				.phone(userProfile.getPhone())
				.email(userProfile.getEmail())
				.avatarUrl(userProfile.getAvatarUrl())
				.studentNo(userProfile.getStudentNo())
				.employeeNo(userProfile.getEmployeeNo())
				.departmentName(userProfile.getDepartmentName())
				.className(userProfile.getClassName())
				.titleName(userProfile.getTitleName())
				.accountStatus(userAccount.getAccountStatus())
				.profileStatus(userProfile.getProfileStatus())
				.build();
	}

	public static LoginSessionItemResponse toLoginSessionItemResponse(LoginSessionDO loginSessionDO) {
		return LoginSessionItemResponse.builder()
				.sessionNo(loginSessionDO.getSessionNo())
				.sessionStatus(loginSessionDO.getSessionStatus())
				.loginAt(loginSessionDO.getLoginAt())
				.expireAt(loginSessionDO.getExpireAt())
				.logoutAt(loginSessionDO.getLogoutAt())
				.ipAddress(loginSessionDO.getIpAddress())
				.deviceType(loginSessionDO.getDeviceType())
				.userAgent(loginSessionDO.getUserAgent())
				.build();
	}

	public static ResourcePermissionResponse toResourcePermissionResponse(ResourcePermissionDO resourcePermissionDO) {
		ResourceType resourceType = ResourceType.fromCode(resourcePermissionDO.getResourceType());
		return ResourcePermissionResponse.builder()
				.id(resourcePermissionDO.getId())
				.resourceType(resourceType.getCode())
				.resourceTypeName(resourceType.getDescription())
				.resourceId(resourcePermissionDO.getResourceId())
				.actionCode(resourcePermissionDO.getActionCode())
				.permissionStatus(resourcePermissionDO.getPermissionStatus())
				.effectiveAt(resourcePermissionDO.getEffectiveAt())
				.expireAt(resourcePermissionDO.getExpireAt())
				.description(resourcePermissionDO.getDescription())
				.build();
	}
}
