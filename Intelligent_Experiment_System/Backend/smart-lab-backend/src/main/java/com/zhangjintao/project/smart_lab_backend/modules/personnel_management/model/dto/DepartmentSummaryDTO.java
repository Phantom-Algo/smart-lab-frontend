package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DepartmentSummaryDTO {

	Long departmentId;
	String departmentNo;
	String departmentCode;
	String departmentName;
	Integer departmentStatus;
}
