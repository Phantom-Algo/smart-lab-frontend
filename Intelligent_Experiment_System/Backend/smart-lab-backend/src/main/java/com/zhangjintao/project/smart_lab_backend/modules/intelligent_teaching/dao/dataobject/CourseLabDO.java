package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_lab")
public class CourseLabDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long courseId;
	private Long labId;
	private Integer bindStatus;
	private LocalDateTime boundAt;
	private LocalDateTime unboundAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
