package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_close_schedule")
public class LabCloseScheduleDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long labId;
	private Integer closeType;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String reason;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
