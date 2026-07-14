package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthProfileErrorCode implements ErrorCode {

	INVALID_CREDENTIALS(61000, "用户名或密码错误", HttpStatus.BAD_REQUEST),
	ACCOUNT_DISABLED(61001, "账号已停用", HttpStatus.BAD_REQUEST),
	ACCOUNT_LOCKED(61002, "账号已锁定", HttpStatus.BAD_REQUEST),
	SESSION_NOT_FOUND(61003, "登录会话不存在或已失效", HttpStatus.UNAUTHORIZED),
	PROFILE_NOT_FOUND(61004, "个人信息不存在", HttpStatus.NOT_FOUND),
	PASSWORD_RULE_INVALID(61005, "密码不符合规则", HttpStatus.BAD_REQUEST),
	OLD_PASSWORD_MISMATCH(61006, "原密码错误", HttpStatus.BAD_REQUEST),
	PHONE_ALREADY_EXISTS(61007, "手机号已被占用", HttpStatus.BAD_REQUEST),
	EMAIL_ALREADY_EXISTS(61008, "邮箱已被占用", HttpStatus.BAD_REQUEST),
	RESOURCE_PERMISSION_DENIED(61009, "资源权限不足", HttpStatus.FORBIDDEN),
	INVALID_TOKEN(61010, "认证令牌无效", HttpStatus.UNAUTHORIZED),
	PERSON_ACCOUNT_ALREADY_BOUND(61011, "人员已绑定账号", HttpStatus.BAD_REQUEST),
	ACCOUNT_BINDING_NOT_FOUND(61012, "账号绑定关系不存在", HttpStatus.NOT_FOUND);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	AuthProfileErrorCode(int code, String message, HttpStatus httpStatus) {
		this.code = code;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
