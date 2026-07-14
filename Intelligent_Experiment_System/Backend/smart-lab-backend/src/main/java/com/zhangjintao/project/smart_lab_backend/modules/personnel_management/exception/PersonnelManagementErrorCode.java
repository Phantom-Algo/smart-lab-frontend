package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PersonnelManagementErrorCode implements ErrorCode {

	DEPARTMENT_NOT_FOUND(62000, "部门不存在", HttpStatus.NOT_FOUND),
	POSITION_NOT_FOUND(62001, "岗位不存在", HttpStatus.NOT_FOUND),
	PERSON_NOT_FOUND(62002, "人员不存在", HttpStatus.NOT_FOUND),
	PERSON_NO_DUPLICATE(62003, "人员编号重复", HttpStatus.BAD_REQUEST),
	STUDENT_NO_DUPLICATE(62004, "学号重复", HttpStatus.BAD_REQUEST),
	EMPLOYEE_NO_DUPLICATE(62005, "工号重复", HttpStatus.BAD_REQUEST),
	PERSON_ACCOUNT_ALREADY_BOUND(62006, "人员已绑定账号", HttpStatus.BAD_REQUEST),
	PERSON_STATUS_NOT_ALLOW_CREATE_ACCOUNT(62007, "当前人员状态不允许创建账号", HttpStatus.BAD_REQUEST),
	DEPARTMENT_HAS_CHILDREN(62008, "部门存在下级部门", HttpStatus.BAD_REQUEST),
	DEPARTMENT_IN_USE(62009, "部门正在被使用", HttpStatus.BAD_REQUEST),
	POSITION_IN_USE(62010, "岗位正在被使用", HttpStatus.BAD_REQUEST),
	POSITION_TYPE_MISMATCH(62011, "岗位类型与人员类型不匹配", HttpStatus.BAD_REQUEST),
	INVALID_PARENT_DEPARTMENT(62012, "无效的上级部门", HttpStatus.BAD_REQUEST),
	PERSON_REQUIRED_FIELD_MISSING(62013, "人员关键信息缺失", HttpStatus.BAD_REQUEST);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	PersonnelManagementErrorCode(int code, String message, HttpStatus httpStatus) {
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
