package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class MessageInboxException extends BusinessException {

	public MessageInboxException(MessageInboxErrorCode errorCode) {
		super(errorCode);
	}

	public MessageInboxException(MessageInboxErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
