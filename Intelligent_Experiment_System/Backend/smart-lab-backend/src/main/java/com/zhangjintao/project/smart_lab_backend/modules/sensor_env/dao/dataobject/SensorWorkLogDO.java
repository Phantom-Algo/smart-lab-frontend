package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传感器工作日志表映射对象。
 * <p>
 * 每条日志对应一次数据上报，数据以 JSON 格式存储，日志一旦写入不可修改。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Data
@TableName("sensor_work_log")
public class SensorWorkLogDO {

	/** 主键 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/** 传感器ID，逻辑关联 sensor.id */
	private Long sensorId;

	/** 传感器编号（冗余字段，便于直接查询） */
	private String sensorNo;

	/** 检测数据快照，JSON格式 */
	private String detectedData;

	/** 数据状态：1=正常 2=异常 */
	private Integer status;

	/** 传感器采集时间 */
	private LocalDateTime detectedAt;

	/** 写入数据库时间 */
	private LocalDateTime createdAt;
}
