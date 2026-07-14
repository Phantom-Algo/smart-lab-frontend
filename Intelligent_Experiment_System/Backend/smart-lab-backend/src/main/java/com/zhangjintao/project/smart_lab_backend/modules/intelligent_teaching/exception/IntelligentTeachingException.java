package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class IntelligentTeachingException extends BusinessException {

	public IntelligentTeachingException(IntelligentTeachingErrorCode errorCode) {
		super(errorCode);
	}

	public IntelligentTeachingException(IntelligentTeachingErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
