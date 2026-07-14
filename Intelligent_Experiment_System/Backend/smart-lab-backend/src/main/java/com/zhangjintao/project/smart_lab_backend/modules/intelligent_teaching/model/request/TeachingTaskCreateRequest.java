package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TeachingTaskCreateRequest {

	@NotNull
	private Long courseId;
	@NotBlank
	private String taskTitle;
	@NotBlank
	private String taskContent;
	@NotNull
	private Integer submitType;
	@NotNull
	@Min(1)
	private Integer maxFileSize;
	@NotNull
	private LocalDateTime deadline;
	private BigDecimal taskWeight;
}
