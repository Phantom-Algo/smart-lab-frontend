package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CourseLabBindingResponse {

	Long labId;
	Integer bindStatus;
	Boolean labDetailAvailable;
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
	LocalDateTime boundAt;
	LocalDateTime unboundAt;
}
