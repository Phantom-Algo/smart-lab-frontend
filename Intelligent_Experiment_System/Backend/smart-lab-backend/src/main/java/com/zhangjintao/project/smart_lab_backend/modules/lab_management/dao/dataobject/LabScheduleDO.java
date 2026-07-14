package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("lab_schedule")
public class LabScheduleDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long labId;
	private Integer dayOfWeek;
	private LocalTime openTime;
	private LocalTime closeTime;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
