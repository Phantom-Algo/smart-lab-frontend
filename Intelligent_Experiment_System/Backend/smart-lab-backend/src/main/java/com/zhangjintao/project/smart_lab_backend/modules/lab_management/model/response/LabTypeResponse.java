package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class LabTypeResponse {

	Long id;
	String labTypeName;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
