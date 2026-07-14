package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import lombok.Data;

@Data
public class CourseQueryRequest {

	private String keyword;
	private String semester;
	private Long teacherId;
	private Long studentId;
	private Long departmentId;
	private Integer courseStatus;
	private Long pageNum = 1L;
	private Long pageSize = 10L;
}
