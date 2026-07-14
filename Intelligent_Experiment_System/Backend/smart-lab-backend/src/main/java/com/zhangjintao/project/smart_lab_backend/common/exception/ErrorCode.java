package com.zhangjintao.project.smart_lab_backend.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	int getCode();

	String getMessage();

	HttpStatus getHttpStatus();
}
