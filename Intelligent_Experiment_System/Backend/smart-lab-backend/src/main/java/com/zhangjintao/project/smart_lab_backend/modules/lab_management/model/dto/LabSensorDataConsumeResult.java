package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionAlertItem;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabSensorDataConsumeResult {

	private String eventNo;
	private String messageId;
	private Lab lab;
	private SensorDataMessage.SensorInfo sensor;
	private LocalDateTime detectedAt;
	private DetectionStatus status;
	private List<DetectionAlertItem> alertItems;
	private List<String> unknownIndicatorNames;
	private LabDetectionLog detectionLog;
	private boolean duplicate;

	public boolean hasAlert() {
		return status == DetectionStatus.ALERT && alertItems != null && !alertItems.isEmpty();
	}
}
