package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class AuthProfileException extends BusinessException {

	public AuthProfileException(AuthProfileErrorCode errorCode) {
		super(errorCode);
	}

	public AuthProfileException(AuthProfileErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
