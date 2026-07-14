package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class SecurityOverviewResponse {

	Integer failedLoginCount;
	Integer accountStatus;
	LocalDateTime lockExpiresAt;
	LocalDateTime lastLoginAt;
	String lastLoginIp;
	LocalDateTime lastPasswordChangedAt;
	Boolean passwordResetRequired;
	List<LoginSessionItemResponse> recentSessions;
}
