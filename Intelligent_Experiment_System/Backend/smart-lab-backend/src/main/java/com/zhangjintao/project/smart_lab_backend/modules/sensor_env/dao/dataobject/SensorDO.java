package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传感器表映射对象。
 */
@Data
@TableName("sensor")
public class SensorDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String sensorNo;
	private Long sensorTypeId;
	private Integer targetType;
	private Long targetId;
	private Integer runningStatus;
	private LocalDateTime lastReportAt;
	private String remark;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
