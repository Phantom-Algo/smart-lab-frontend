package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourseQuery {

	String keyword;
	String semester;
	Long teacherId;
	Long studentId;
	Long departmentId;
	Integer courseStatus;
	Long pageNum;
	Long pageSize;
}
