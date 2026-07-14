package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class PendingReservationQueryCriteria {

	Long labId;
	LocalDate startDate;
	LocalDate endDate;
	Long page;
	Long pageSize;
}
