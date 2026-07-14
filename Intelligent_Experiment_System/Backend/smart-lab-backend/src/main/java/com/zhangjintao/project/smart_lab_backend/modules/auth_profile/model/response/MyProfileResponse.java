package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MyProfileResponse {

	Long userId;
	Long profileId;
	Long personId;
	String username;
	Integer userType;
	String userTypeName;
	String realName;
	Integer genderType;
	String phone;
	String email;
	String avatarUrl;
	String studentNo;
	String employeeNo;
	String departmentName;
	String className;
	String titleName;
	Integer accountStatus;
	Integer profileStatus;
}
