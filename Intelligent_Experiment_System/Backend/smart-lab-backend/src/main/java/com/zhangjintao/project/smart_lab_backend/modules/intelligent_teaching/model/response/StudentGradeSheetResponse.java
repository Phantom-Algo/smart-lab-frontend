package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class StudentGradeSheetResponse {

	Long courseId;
	String courseName;
	Long studentId;
	String studentName;
	String studentNo;
	Integer publishStatus;
	BigDecimal totalScore;
	List<StudentGradeItemResponse> items;
}
