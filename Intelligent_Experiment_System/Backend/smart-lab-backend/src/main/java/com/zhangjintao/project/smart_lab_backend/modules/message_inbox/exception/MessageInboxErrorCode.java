package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MessageInboxErrorCode implements ErrorCode {

	INVALID_MESSAGE_EVENT(60000, "消息事件参数错误", HttpStatus.BAD_REQUEST),
	TEMPLATE_NOT_FOUND(60001, "消息模板不存在或未启用", HttpStatus.BAD_REQUEST),
	TEMPLATE_PARAM_INVALID(60002, "消息模板参数错误", HttpStatus.BAD_REQUEST),
	RECIPIENT_RESOLVE_FAILED(60003, "消息接收人解析失败", HttpStatus.BAD_REQUEST),
	MESSAGE_DISPATCH_FAILED(60004, "消息分发失败", HttpStatus.INTERNAL_SERVER_ERROR),
	TEMPLATE_ALREADY_EXISTS(60005, "消息模板编码已存在", HttpStatus.CONFLICT),
	INBOX_MESSAGE_NOT_FOUND(60006, "收件箱消息不存在", HttpStatus.NOT_FOUND);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	MessageInboxErrorCode(int code, String message, HttpStatus httpStatus) {
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
