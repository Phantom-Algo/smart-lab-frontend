package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourseStudentResponse {

	Long personId;
	String personName;
	String studentNo;
	String className;
	Long userId;
	Integer accountStatus;
	Integer enrollStatus;
}
