package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class LabResponse {

	Long id;
	String labName;
	Long labTypeId;
	String labTypeName;
	String campus;
	String building;
	Integer floor;
	String roomNo;
	Integer capacity;
	BigDecimal areaM2;
	Long dutyUserId;
	String dutyUserName;
	Integer runningStatus;
	String runningStatusDesc;
	Integer openStatus;
	String openStatusDesc;
	String description;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
