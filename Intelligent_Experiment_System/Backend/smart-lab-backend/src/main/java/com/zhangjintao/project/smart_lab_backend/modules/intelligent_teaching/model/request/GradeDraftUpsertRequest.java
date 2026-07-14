package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeDraftUpsertRequest {

	@NotNull
	private Long gradeItemId;
	@NotNull
	private Long studentId;
	private Long taskSubmissionId;
	@NotNull
	private BigDecimal score;
	private String comment;
}
