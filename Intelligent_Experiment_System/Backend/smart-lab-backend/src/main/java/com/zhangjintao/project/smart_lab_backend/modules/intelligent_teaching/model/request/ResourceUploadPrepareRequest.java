package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceUploadPrepareRequest {

	@NotBlank
	private String fileName;
	private Long fileSize;
	private String contentType;
}
