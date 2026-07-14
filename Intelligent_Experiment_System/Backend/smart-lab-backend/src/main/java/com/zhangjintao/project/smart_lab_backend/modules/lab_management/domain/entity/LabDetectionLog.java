package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabDetectionLog {

	private Long id;
	private String eventNo;
	private String messageId;
	private Long labId;
	private Long sensorId;
	private String sensorNo;
	private String detectedData;
	private LocalDateTime detectedAt;
	private DetectionStatus status;
	private String alertDetail;
	private LocalDateTime createdAt;
}
