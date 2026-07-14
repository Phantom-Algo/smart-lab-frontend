package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PersonDirectoryQuery {

	List<Integer> personTypes;
	Long departmentId;
	Long positionId;
	List<Integer> personStatuses;
	String keyword;
	Boolean hasAccount;
	Boolean onlyEnabledDepartment;
	Long pageNum;
	Long pageSize;
}
