package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class LabCloseScheduleQueryCriteria {

	LocalDate startDate;
	LocalDate endDate;
	Integer closeType;
}
