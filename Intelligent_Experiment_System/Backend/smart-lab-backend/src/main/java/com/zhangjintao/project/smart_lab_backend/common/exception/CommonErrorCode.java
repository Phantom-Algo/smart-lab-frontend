package com.zhangjintao.project.smart_lab_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

	BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
	PARAM_VALIDATION_FAILED(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED(40100, "用户未认证", HttpStatus.UNAUTHORIZED),
	FORBIDDEN(40300, "无访问权限", HttpStatus.FORBIDDEN),
	NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
	UNSUPPORTED_MEDIA_TYPE(41500, "请求媒体类型不支持", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	INTERNAL_SERVER_ERROR(50000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	CommonErrorCode(int code, String message, HttpStatus httpStatus) {
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
