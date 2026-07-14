package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LoginSessionItemResponse {

	String sessionNo;
	Integer sessionStatus;
	LocalDateTime loginAt;
	LocalDateTime expireAt;
	LocalDateTime logoutAt;
	String ipAddress;
	String deviceType;
	String userAgent;
}
