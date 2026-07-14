package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountBindingDTO {

	Long userId;
	Long personId;
	String username;
	Integer userType;
	Integer accountStatus;
	Boolean passwordResetRequired;
	String temporaryPassword;
}
