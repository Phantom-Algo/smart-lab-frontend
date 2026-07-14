package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonAccountSummaryDTO;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PersonDetailResponse {

	Long id;
	String personNo;
	String personName;
	Integer personType;
	Integer genderType;
	String phone;
	String email;
	String idCardNoMasked;
	String studentNo;
	String employeeNo;
	Long departmentId;
	String departmentName;
	Long positionId;
	String positionName;
	String className;
	String titleName;
	Integer personStatus;
	LocalDateTime entryAt;
	LocalDateTime leaveAt;
	Integer sourceType;
	String description;
	PersonAccountSummaryDTO account;
}
