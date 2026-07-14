package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LabStatusUpdateCommand {

	Integer runningStatus;
	Integer openStatus;
	String changeReason;
	Long operatorId;
	boolean forceFinishCurrentUsage;
	boolean forceCancelFutureReservations;
	boolean confirmPermanentClose;
}
