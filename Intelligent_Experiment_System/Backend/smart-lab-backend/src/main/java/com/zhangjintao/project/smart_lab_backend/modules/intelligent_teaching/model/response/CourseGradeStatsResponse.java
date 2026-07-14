package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CourseGradeStatsResponse {

	Long courseId;
	long studentCount;
	long publishedStudentCount;
	long draftStudentCount;
	BigDecimal averageScore;
	BigDecimal highestScore;
	BigDecimal lowestScore;
}
