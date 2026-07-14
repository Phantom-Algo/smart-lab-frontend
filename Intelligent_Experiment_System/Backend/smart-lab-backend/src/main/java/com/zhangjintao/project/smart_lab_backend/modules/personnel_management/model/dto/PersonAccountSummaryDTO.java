package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonAccountSummaryDTO {

	Long userId;
	String username;
	Integer userType;
	Integer accountStatus;
	Boolean passwordResetRequired;
	String temporaryPassword;
}
