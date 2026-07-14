package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import lombok.Data;

@Data
public class PersonCreateAccountRequest {

	private String username;
	private String initialPassword;
	private Boolean autoGeneratePassword = Boolean.TRUE;
}
