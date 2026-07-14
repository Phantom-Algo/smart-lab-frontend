package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_grade_publish")
public class CourseGradePublishDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long courseId;
	private Integer publishStatus;
	private Long publishedBy;
	private LocalDateTime publishedAt;
	private Long lockedBy;
	private LocalDateTime lockedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
