package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 传感器数据路由转发命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataRouteCommand {

	private Long logId;
	private Long sensorId;
	private String sensorNo;
	private Long sensorTypeId;
	private String sensorTypeName;
	private Integer targetType;
	private Long targetId;
	private Map<String, SensorDataMessage.DataItem> detectedData;
	private LocalDateTime detectedAt;
}
