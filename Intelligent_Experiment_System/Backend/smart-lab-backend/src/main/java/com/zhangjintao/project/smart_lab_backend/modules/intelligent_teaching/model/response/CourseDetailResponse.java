package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourseDetailResponse {

	Long courseId;
	String courseCode;
	String courseName;
	String semester;
	Integer credit;
	Integer totalHours;
	Long teacherId;
	String teacherName;
	Long departmentId;
	String departmentName;
	String description;
	String syllabus;
	Integer courseStatus;
	Long studentCount;
}
