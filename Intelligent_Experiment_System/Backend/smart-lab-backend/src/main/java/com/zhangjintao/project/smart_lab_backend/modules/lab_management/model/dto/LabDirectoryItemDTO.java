package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LabDirectoryItemDTO {

	Long labId;
	String labName;
	String labNo;
	String campus;
	String building;
	Integer floor;
	String roomNo;
	Integer capacity;
	Integer runningStatus;
	String runningStatusDesc;
	Integer openStatus;
	String openStatusDesc;
	Boolean available;
}
