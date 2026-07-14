package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMyProfileRequest {

	@Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
	private String phone;

	@Email(message = "邮箱格式不正确")
	private String email;

	@Size(max = 255, message = "头像地址长度不能超过255")
	private String avatarUrl;
}
