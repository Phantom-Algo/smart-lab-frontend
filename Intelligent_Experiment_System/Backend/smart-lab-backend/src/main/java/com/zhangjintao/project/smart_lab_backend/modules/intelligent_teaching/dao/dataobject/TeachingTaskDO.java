package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("teaching_task")
public class TeachingTaskDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String taskNo;
	private Long courseId;
	private String taskTitle;
	private String taskContent;
	private Integer submitType;
	private Integer maxFileSize;
	private LocalDateTime deadline;
	private BigDecimal taskWeight;
	private Integer taskStatus;
	private LocalDateTime publishedAt;
	private Long createdBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
