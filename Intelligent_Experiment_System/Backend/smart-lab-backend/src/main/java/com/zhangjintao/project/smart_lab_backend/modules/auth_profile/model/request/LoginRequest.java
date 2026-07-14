package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

	@NotBlank(message = "登录名不能为空")
	private String username;

	@NotBlank(message = "密码不能为空")
	private String password;
}
