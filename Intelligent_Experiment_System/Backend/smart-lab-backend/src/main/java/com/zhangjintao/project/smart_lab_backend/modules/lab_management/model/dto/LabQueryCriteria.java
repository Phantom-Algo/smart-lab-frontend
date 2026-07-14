package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LabQueryCriteria {

	String keyword;
	String campus;
	String building;
	Integer floor;
	String roomNo;
	Long labTypeId;
	Integer runningStatus;
	Integer openStatus;
	Long page;
	Long pageSize;
	String sortField;
	String sortOrder;
}
