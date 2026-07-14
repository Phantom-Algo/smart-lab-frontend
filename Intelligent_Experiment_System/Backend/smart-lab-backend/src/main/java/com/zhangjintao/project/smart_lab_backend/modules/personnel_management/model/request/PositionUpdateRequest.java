package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PositionUpdateRequest {

	@NotBlank(message = "岗位名称不能为空")
	private String positionName;

	@NotNull(message = "岗位类型不能为空")
	private Integer positionType;

	@NotNull(message = "所属部门不能为空")
	private Long departmentId;

	@NotNull(message = "岗位状态不能为空")
	private Integer positionStatus;

	private Integer sortNo;

	private String description;
}
