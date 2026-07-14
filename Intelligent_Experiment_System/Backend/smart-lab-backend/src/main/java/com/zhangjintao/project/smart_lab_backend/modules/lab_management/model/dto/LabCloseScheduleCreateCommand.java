package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LabCloseScheduleCreateCommand {

	Integer closeType;
	LocalDateTime startTime;
	LocalDateTime endTime;
	String reason;
	boolean forceCancelConflictedReservations;
}
