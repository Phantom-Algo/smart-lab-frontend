package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LabCloseScheduleMutationResponse {

	boolean applied;
	LabCloseScheduleResponse closeSchedule;
	List<ConflictedReservationResponse> conflictedReservations;
}
