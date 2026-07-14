package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class CourseDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String courseCode;
	private String courseName;
	private String semester;
	private Integer credit;
	private Integer totalHours;
	private Long teacherId;
	private Long departmentId;
	private String description;
	private String syllabus;
	private Integer courseStatus;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
