package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class LabPageItemDTO {

	Long id;
	String labName;
	Long labTypeId;
	LabLocation location;
	Integer capacity;
	BigDecimal areaM2;
	Long dutyUserId;
	LabRunningStatus runningStatus;
	LabOpenStatus openStatus;
	String description;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
