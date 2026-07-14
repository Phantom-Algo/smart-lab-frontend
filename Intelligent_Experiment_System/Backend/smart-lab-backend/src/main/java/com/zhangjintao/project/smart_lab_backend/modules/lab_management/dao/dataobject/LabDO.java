package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lab")
public class LabDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String labName;
	private Long labTypeId;
	private String campus;
	private String building;
	private Integer floor;
	private String roomNo;
	private Integer capacity;
	private BigDecimal areaM2;
	private Long dutyUserId;
	private Integer runningStatus;
	private Integer openStatus;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
