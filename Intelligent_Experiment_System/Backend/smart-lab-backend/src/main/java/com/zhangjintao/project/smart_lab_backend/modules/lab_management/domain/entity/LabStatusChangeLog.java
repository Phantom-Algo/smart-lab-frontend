package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabStatusChangeLog {

	private Long id;
	private Long labId;
	private Long operatorId;
	private LabRunningStatus fromRunningStatus;
	private LabRunningStatus toRunningStatus;
	private LabOpenStatus fromOpenStatus;
	private LabOpenStatus toOpenStatus;
	private String changeReason;
	private String triggerSource;
	private Integer forcedCheckoutCount;
	private Integer cancelledReservationCount;
	private LocalDateTime createdAt;
}
