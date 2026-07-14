package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class PersonnelManagementException extends BusinessException {

	public PersonnelManagementException(PersonnelManagementErrorCode errorCode) {
		super(errorCode);
	}

	public PersonnelManagementException(PersonnelManagementErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
