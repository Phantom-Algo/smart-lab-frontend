package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class LabScheduleItemCommand {

	Integer dayOfWeek;
	LocalTime openTime;
	LocalTime closeTime;
}
