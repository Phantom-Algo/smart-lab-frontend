package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GradeItemResponse {

	Long gradeItemId;
	Long courseId;
	String itemName;
	Integer itemType;
	BigDecimal itemWeight;
	BigDecimal fullScore;
	Long relatedTaskId;
	Integer sortNo;
}
