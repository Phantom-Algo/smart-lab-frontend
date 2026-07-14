package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLabTypeRequest {

	@NotBlank(message = "实验室类型名称不能为空")
	@Size(max = 64, message = "实验室类型名称长度不能超过64")
	private String labTypeName;
}
