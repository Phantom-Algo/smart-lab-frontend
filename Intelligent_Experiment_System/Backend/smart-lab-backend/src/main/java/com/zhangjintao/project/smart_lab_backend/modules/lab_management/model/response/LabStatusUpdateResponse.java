package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LabStatusUpdateResponse {

	boolean applied;
	LabDetailResponse lab;
	List<ConflictedReservationResponse> conflictedReservations;
	List<ConflictedReservationResponse> cancelledReservations;
	List<ConflictedReservationResponse> forcedCheckedOutReservations;
	String message;
}
