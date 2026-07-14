package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sensor_type")
public class SensorTypeDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String sensorTypeName;
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
