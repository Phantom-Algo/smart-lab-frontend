package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseBindLabRequest {

	@NotNull
	private Long labId;
}
