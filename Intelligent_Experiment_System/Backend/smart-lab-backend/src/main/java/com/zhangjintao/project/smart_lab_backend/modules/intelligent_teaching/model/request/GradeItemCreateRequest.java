package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeItemCreateRequest {

	@NotBlank
	private String itemName;
	@NotNull
	private Integer itemType;
	@NotNull
	private BigDecimal itemWeight;
	@NotNull
	private BigDecimal fullScore;
	private Long relatedTaskId;
	private Integer sortNo = 0;
}
