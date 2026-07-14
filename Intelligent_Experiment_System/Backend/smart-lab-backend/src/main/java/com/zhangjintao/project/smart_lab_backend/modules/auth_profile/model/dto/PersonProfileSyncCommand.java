package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonProfileSyncCommand {

	Long personId;
	String realName;
	Integer genderType;
	String phone;
	String email;
	String studentNo;
	String employeeNo;
	String departmentName;
	String className;
	String titleName;
}
