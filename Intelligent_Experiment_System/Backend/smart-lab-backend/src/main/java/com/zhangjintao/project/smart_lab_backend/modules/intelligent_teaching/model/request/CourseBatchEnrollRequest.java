package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CourseBatchEnrollRequest {

	@NotEmpty
	private List<Long> studentIds;
}
