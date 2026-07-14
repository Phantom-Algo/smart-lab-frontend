package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LabCloseScheduleResponse {

	Long id;
	Long labId;
	Integer closeType;
	String closeTypeDesc;
	LocalDateTime startTime;
	LocalDateTime endTime;
	String reason;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
