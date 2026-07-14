package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentCreateRequest {

	@NotBlank(message = "部门编号不能为空")
	private String departmentNo;

	@NotBlank(message = "部门编码不能为空")
	private String departmentCode;

	@NotBlank(message = "部门名称不能为空")
	private String departmentName;

	private Long parentId;

	private Long leaderPersonId;

	@NotNull(message = "部门状态不能为空")
	private Integer departmentStatus;

	private Integer sortNo;

	private String description;
}
