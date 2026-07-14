package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseUpdateRequest {

	@NotBlank
	private String courseName;
	@NotBlank
	private String semester;
	@NotNull
	@Min(1)
	private Integer credit;
	@NotNull
	@Min(1)
	private Integer totalHours;
	@NotNull
	private Long teacherId;
	private Long departmentId;
	private String description;
	private String syllabus;
	private Integer courseStatus;
}
