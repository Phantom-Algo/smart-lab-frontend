package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_sensor_binding")
public class LabSensorBindingDO {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long labId;
	private Long sensorId;
	private Integer bindStatus;
	private LocalDateTime boundAt;
	private LocalDateTime unboundAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
