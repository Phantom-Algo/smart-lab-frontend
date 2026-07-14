package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class StudentGradeItemResponse {

	Long gradeItemId;
	String itemName;
	Integer itemType;
	BigDecimal itemWeight;
	BigDecimal fullScore;
	BigDecimal score;
	BigDecimal weightedScore;
	Integer gradeStatus;
	String comment;
}
