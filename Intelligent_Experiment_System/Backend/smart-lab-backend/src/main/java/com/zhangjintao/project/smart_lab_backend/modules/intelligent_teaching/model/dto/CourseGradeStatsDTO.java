package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CourseGradeStatsDTO {

	Long courseId;
	long studentCount;
	long publishedStudentCount;
	long draftStudentCount;
	BigDecimal averageScore;
	BigDecimal highestScore;
	BigDecimal lowestScore;
}
