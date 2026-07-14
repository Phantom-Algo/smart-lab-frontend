package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum IntelligentTeachingErrorCode implements ErrorCode {

	COURSE_NOT_FOUND(63000, "课程不存在", HttpStatus.NOT_FOUND),
	COURSE_CODE_DUPLICATE(63001, "课程编码重复", HttpStatus.BAD_REQUEST),
	COURSE_NAME_DUPLICATE_IN_SEMESTER(63002, "同学期课程名称重复", HttpStatus.BAD_REQUEST),
	COURSE_TEACHER_INVALID(63003, "授课教师不存在或类型非法", HttpStatus.BAD_REQUEST),
	COURSE_DEPARTMENT_INVALID(63004, "开课院系不存在", HttpStatus.BAD_REQUEST),
	COURSE_STUDENT_INVALID(63005, "课程学生不存在或类型非法", HttpStatus.BAD_REQUEST),
	COURSE_STUDENT_EMPTY(63006, "课程学生不能为空", HttpStatus.BAD_REQUEST),
	COURSE_LAB_ALREADY_BOUND(63007, "课程与实验室已绑定", HttpStatus.BAD_REQUEST),
	TEACHING_TASK_NOT_FOUND(63008, "教学任务不存在", HttpStatus.NOT_FOUND),
	TEACHING_TASK_STATUS_INVALID(63009, "教学任务当前状态不允许执行该操作", HttpStatus.BAD_REQUEST),
	TEACHING_TASK_DEADLINE_INVALID(63010, "教学任务截止时间非法", HttpStatus.BAD_REQUEST),
	TEACHING_RESOURCE_NOT_FOUND(63011, "教学资源不存在", HttpStatus.NOT_FOUND),
	TEACHING_RESOURCE_CONTENT_INVALID(63012, "教学资源文件或链接信息非法", HttpStatus.BAD_REQUEST),
	CURRENT_USER_NOT_ALLOWED(63013, "当前用户无权限执行该操作", HttpStatus.FORBIDDEN),
	LAB_DIRECTORY_UNAVAILABLE(63014, "实验室目录服务暂未接入", HttpStatus.BAD_REQUEST),
	UPLOAD_PREPARE_NOT_SUPPORTED(63015, "当前仅支持预留上传参数，不支持直接文件上传", HttpStatus.BAD_REQUEST),
	TASK_SUBMISSION_NOT_FOUND(63016, "任务提交不存在", HttpStatus.NOT_FOUND),
	TASK_SUBMISSION_CONTENT_INVALID(63017, "任务提交内容不符合要求", HttpStatus.BAD_REQUEST),
	TASK_SUBMISSION_NOT_ALLOWED(63018, "当前用户不允许提交该任务", HttpStatus.FORBIDDEN),
	TASK_SUBMISSION_RETURN_REASON_REQUIRED(63019, "退回原因不能为空", HttpStatus.BAD_REQUEST),
	GRADE_ITEM_NOT_FOUND(63020, "评定项不存在", HttpStatus.NOT_FOUND),
	GRADE_ITEM_NAME_DUPLICATE(63021, "评定项名称重复", HttpStatus.BAD_REQUEST),
	GRADE_ITEM_WEIGHT_INVALID(63022, "评定项权重配置非法", HttpStatus.BAD_REQUEST),
	GRADE_ITEM_RELATED_TASK_INVALID(63023, "评定项绑定任务非法", HttpStatus.BAD_REQUEST),
	GRADE_ASSESSMENT_NOT_FOUND(63024, "成绩记录不存在", HttpStatus.NOT_FOUND),
	GRADE_SCORE_INVALID(63025, "成绩分值非法", HttpStatus.BAD_REQUEST),
	GRADE_PUBLISH_NOT_READY(63026, "课程成绩尚未满足发布条件", HttpStatus.BAD_REQUEST),
	GRADE_ALREADY_PUBLISHED(63027, "课程成绩已发布，不允许继续修改", HttpStatus.BAD_REQUEST),
	GRADE_SHEET_NOT_FOUND(63028, "成绩单不存在", HttpStatus.NOT_FOUND),
	COURSE_LAB_NOT_FOUND(63029, "实验室不存在", HttpStatus.NOT_FOUND),
	COURSE_LAB_UNAVAILABLE(63030, "实验室当前不可用于课程绑定", HttpStatus.BAD_REQUEST);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	IntelligentTeachingErrorCode(int code, String message, HttpStatus httpStatus) {
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
