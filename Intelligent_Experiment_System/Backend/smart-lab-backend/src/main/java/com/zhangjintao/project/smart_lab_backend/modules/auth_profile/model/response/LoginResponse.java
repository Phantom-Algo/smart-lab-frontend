package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LoginResponse {

	Long userId;
	String username;
	Integer userType;
	String userTypeName;
	String accessToken;
	String tokenType;
	LocalDateTime expireAt;
	Boolean passwordResetRequired;
}
