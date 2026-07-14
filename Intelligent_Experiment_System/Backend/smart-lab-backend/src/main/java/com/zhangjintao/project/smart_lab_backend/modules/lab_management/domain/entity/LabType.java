package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabType {

	private Long id;
	private String labTypeName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static LabType create(LabTypeCreateCommand command) {
		return LabType.builder()
				.labTypeName(command.getLabTypeName())
				.build();
	}

	public void applyUpdate(LabTypeUpdateCommand command) {
		this.labTypeName = command.getLabTypeName();
	}
}
