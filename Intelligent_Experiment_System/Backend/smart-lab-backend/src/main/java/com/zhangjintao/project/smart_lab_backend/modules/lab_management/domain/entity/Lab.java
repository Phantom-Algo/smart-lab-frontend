package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabUpdateCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lab {

	private Long id;
	private String labName;
	private Long labTypeId;
	private LabLocation location;
	private Integer capacity;
	private BigDecimal areaM2;
	private Long dutyUserId;
	private LabRunningStatus runningStatus;
	private LabOpenStatus openStatus;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	public static Lab create(LabCreateCommand command) {
		return Lab.builder()
				.labName(command.getLabName())
				.labTypeId(command.getLabTypeId())
				.location(command.getLocation())
				.capacity(command.getCapacity() == null ? 0 : command.getCapacity())
				.areaM2(command.getAreaM2())
				.dutyUserId(command.getDutyUserId())
				.runningStatus(LabRunningStatus.DISABLED)
				.openStatus(LabOpenStatus.TEMPORARY_CLOSED)
				.description(command.getDescription())
				.build();
	}

	public void applyUpdate(LabUpdateCommand command) {
		if (command.getLabName() != null) {
			this.labName = command.getLabName();
		}
		if (command.getLabTypeId() != null) {
			this.labTypeId = command.getLabTypeId();
		}
		if (command.getLocation() != null) {
			this.location = command.getLocation();
		}
		if (command.getCapacity() != null) {
			this.capacity = command.getCapacity();
		}
		if (command.getAreaM2() != null) {
			this.areaM2 = command.getAreaM2();
		}
		if (command.getDutyUserId() != null) {
			this.dutyUserId = command.getDutyUserId();
		}
		if (command.getDescription() != null) {
			this.description = command.getDescription();
		}
	}

	public void markDeleted(LocalDateTime now) {
		this.runningStatus = LabRunningStatus.DISABLED;
		this.openStatus = LabOpenStatus.PERMANENT_CLOSED;
		this.deletedAt = now;
		this.updatedAt = now;
	}
}
