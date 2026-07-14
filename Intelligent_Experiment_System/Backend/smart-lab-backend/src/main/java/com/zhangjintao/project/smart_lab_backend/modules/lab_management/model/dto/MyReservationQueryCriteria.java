package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MyReservationQueryCriteria {

	Long applicantId;
	Integer status;
	Long page;
	Long pageSize;
}
