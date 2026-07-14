package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_sensor_binding")
public class DeviceSensorBindingDO {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long deviceId;
	private Long sensorId;
	private Integer bindStatus;
	private LocalDateTime boundAt;
	private LocalDateTime unboundAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
