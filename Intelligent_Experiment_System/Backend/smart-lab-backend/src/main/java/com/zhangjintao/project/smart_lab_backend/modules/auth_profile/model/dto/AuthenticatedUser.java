package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import lombok.Builder;
import lombok.Value;

import java.util.Collection;

@Value
@Builder
public class AuthenticatedUser {

	Long userId;
	Long profileId;
	Long personId;
	String username;
	UserType userType;
	Long sessionId;
	String sessionNo;
	Collection<String> authorities;
}
