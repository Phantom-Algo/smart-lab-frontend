package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

	@NotBlank(message = "原密码不能为空")
	private String oldPassword;

	@NotBlank(message = "新密码不能为空")
	private String newPassword;

	@NotBlank(message = "确认密码不能为空")
	private String confirmPassword;
}
