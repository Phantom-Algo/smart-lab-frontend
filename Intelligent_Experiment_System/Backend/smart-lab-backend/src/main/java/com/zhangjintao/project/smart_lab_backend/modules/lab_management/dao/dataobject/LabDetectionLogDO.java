package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_detection_log")
public class LabDetectionLogDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String eventNo;
	private String messageId;
	private Long labId;
	private Long sensorId;
	private String sensorNo;
	private String detectedData;
	private LocalDateTime detectedAt;
	private Integer status;
	private String alertDetail;
	private LocalDateTime createdAt;
}
