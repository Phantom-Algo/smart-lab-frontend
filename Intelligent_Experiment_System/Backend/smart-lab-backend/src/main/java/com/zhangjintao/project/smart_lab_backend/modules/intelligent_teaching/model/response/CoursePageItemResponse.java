package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CoursePageItemResponse {

	Long courseId;
	String courseCode;
	String courseName;
	String semester;
	String teacherName;
	String departmentName;
	Integer courseStatus;
	Long studentCount;
}
