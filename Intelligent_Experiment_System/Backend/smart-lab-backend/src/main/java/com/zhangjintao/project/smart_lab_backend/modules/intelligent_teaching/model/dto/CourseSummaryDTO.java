package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourseSummaryDTO {

	Long courseId;
	String courseCode;
	String courseName;
	String semester;
	Long teacherId;
	String teacherName;
	Long departmentId;
	String departmentName;
	Integer courseStatus;
}
