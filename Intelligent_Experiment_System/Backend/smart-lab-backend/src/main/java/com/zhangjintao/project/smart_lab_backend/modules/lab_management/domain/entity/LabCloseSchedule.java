package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabCloseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabCloseSchedule {

	private Long id;
	private Long labId;
	private LabCloseType closeType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String reason;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void applyUpdate(LabCloseType closeType, LocalDateTime startTime, LocalDateTime endTime, String reason) {
		this.closeType = closeType;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reason = reason;
	}
}
