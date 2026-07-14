package com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class LabManagementException extends BusinessException {

	public LabManagementException(LabManagementErrorCode errorCode) {
		super(errorCode);
	}

	public LabManagementException(LabManagementErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
