package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sensor_data_type")
public class SensorDataTypeDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long sensorTypeId;
	private String dataName;
	private String unit;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
