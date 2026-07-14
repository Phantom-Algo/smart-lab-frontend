package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import lombok.Data;

import java.util.List;

@Data
public class PersonQueryRequest {

	private List<Integer> personTypes;
	private Long departmentId;
	private Long positionId;
	private List<Integer> personStatuses;
	private String keyword;
	private Boolean hasAccount;
	private Boolean onlyEnabledDepartment;
	private Long pageNum = 1L;
	private Long pageSize = 10L;
}
