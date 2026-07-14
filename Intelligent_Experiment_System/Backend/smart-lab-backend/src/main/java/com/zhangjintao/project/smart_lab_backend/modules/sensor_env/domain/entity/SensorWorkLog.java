package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 传感器工作日志领域实体。
 * <p>
 * 每条日志对应一次数据上报，是一种不可变的只读快照。日志一旦写入即不可修改，
 * 因此不提供 update 等可变方法，仅通过静态工厂方法 {@link #create} 构造新实例。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@Getter
@Builder
@AllArgsConstructor
public class SensorWorkLog {

	/** 主键（插入前为 null，插入后由数据库自动生成） */
	private Long id;

	/** 传感器ID */
	private Long sensorId;

	/** 传感器编号（冗余字段） */
	private String sensorNo;

	/** 检测数据快照，key为数据名称，value为数值 */
	private Map<String, BigDecimal> detectedData;

	/** 数据状态：1=正常 */
	private Integer status;

	/** 传感器采集时间 */
	private LocalDateTime detectedAt;

	/** 写入数据库时间 */
	private LocalDateTime createdAt;

	/**
	 * 创建一条新的工作日志实体（未持久化，id=null）。
	 *
	 * @param sensorId     传感器主键ID
	 * @param sensorNo     传感器编号
	 * @param detectedData 检测数据快照
	 * @param detectedAt   传感器采集时间
	 * @return 未持久化的工作日志实体
	 */
	public static SensorWorkLog create(Long sensorId, String sensorNo,
	                                   Map<String, BigDecimal> detectedData, LocalDateTime detectedAt) {
		return SensorWorkLog.builder()
				.sensorId(sensorId)
				.sensorNo(sensorNo)
				.detectedData(Map.copyOf(detectedData))
				.status(1) // 模块三统一写入正常状态，告警判定由下游子系统负责
				.detectedAt(detectedAt)
				.build();
	}
}
