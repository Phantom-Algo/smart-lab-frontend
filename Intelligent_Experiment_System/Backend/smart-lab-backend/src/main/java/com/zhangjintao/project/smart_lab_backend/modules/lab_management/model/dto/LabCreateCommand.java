package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class LabCreateCommand {

	String labName;
	Long labTypeId;
	LabLocation location;
	Integer capacity;
	BigDecimal areaM2;
	Long dutyUserId;
	String description;
}
