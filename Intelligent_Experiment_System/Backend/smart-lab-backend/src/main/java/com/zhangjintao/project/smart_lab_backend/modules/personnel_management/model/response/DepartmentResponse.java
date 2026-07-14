package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DepartmentResponse {

	Long id;
	String departmentNo;
	String departmentCode;
	String departmentName;
	Long parentId;
	Long leaderPersonId;
	Integer departmentStatus;
	Integer sortNo;
	String description;
}
