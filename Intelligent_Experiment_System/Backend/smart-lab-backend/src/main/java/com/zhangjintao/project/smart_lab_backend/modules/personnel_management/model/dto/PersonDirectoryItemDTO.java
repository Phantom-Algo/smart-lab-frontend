package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonDirectoryItemDTO {

	Long personId;
	String personNo;
	String personName;
	Integer personType;
	Integer genderType;
	String phone;
	String email;
	String studentNo;
	String employeeNo;
	Long departmentId;
	String departmentName;
	Long positionId;
	String positionName;
	String className;
	String titleName;
	Integer personStatus;
	Long userId;
	Integer accountStatus;
	String username;
}
