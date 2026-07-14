package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchGradeDraftRequest {

	@NotEmpty
	private List<GradeDraftUpsertRequest> items;
}
