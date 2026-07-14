package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonStatusChangeRequest {

	@NotNull(message = "人员状态不能为空")
	private Integer personStatus;

	private String description;
}
