package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PersonUpdateRequest {

	@NotBlank(message = "人员姓名不能为空")
	private String personName;

	@NotNull(message = "人员类型不能为空")
	private Integer personType;

	@NotNull(message = "性别不能为空")
	private Integer genderType;

	private String phone;
	private String email;
	private String idCardNoMasked;
	private String studentNo;
	private String employeeNo;

	@NotNull(message = "所属部门不能为空")
	private Long departmentId;

	private Long positionId;
	private String className;
	private String titleName;

	@NotNull(message = "人员状态不能为空")
	private Integer personStatus;

	private LocalDateTime entryAt;
	private LocalDateTime leaveAt;

	private String description;
}
