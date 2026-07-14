package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class LabScheduleResponse {

	Long id;
	Integer dayOfWeek;
	LocalTime openTime;
	LocalTime closeTime;
}
