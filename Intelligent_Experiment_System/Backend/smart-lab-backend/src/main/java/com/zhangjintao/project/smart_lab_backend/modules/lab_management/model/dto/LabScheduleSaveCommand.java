package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LabScheduleSaveCommand {

	List<LabScheduleItemCommand> schedules;
	boolean forceCancelConflictedReservations;
}
