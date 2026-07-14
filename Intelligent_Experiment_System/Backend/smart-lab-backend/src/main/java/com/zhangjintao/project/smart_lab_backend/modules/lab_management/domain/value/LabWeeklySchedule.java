package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
@JsonDeserialize(builder = LabWeeklySchedule.LabWeeklyScheduleBuilder.class)
public class LabWeeklySchedule {

	Long id;
	Long labId;
	Integer dayOfWeek;
	LocalTime openTime;
	LocalTime closeTime;

	@JsonPOJOBuilder(withPrefix = "")
	public static class LabWeeklyScheduleBuilder {
	}
}
