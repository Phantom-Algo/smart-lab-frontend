package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DepartmentTreeNodeResponse {

	Long id;
	String departmentNo;
	String departmentName;
	String departmentCode;
	Integer departmentStatus;
	Integer sortNo;
	String leaderPersonName;
	List<DepartmentTreeNodeResponse> children;
}
