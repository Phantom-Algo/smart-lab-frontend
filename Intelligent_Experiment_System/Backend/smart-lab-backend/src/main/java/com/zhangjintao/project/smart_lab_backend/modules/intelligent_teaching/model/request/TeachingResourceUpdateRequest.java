package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeachingResourceUpdateRequest {

	@NotBlank
	private String resourceName;
	@NotNull
	private Integer resourceType;
	private String fileUrl;
	private String externalUrl;
	private Long fileSize;
	private Integer sortNo = 0;
}
