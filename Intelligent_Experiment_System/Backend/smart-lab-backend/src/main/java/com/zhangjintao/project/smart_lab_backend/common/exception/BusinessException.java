package com.zhangjintao.project.smart_lab_backend.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final int code;
	private final HttpStatus httpStatus;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode.getCode(), message, errorCode.getHttpStatus());
	}

	public BusinessException(int code, String message) {
		this(code, message, HttpStatus.BAD_REQUEST);
	}

	public BusinessException(int code, String message, HttpStatus httpStatus) {
		super(message);
		this.code = code;
		this.httpStatus = httpStatus;
	}

	public int getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
