package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants;

public final class MessageInboxConstants {

	private MessageInboxConstants() {
	}

	public static final String STREAM_PAYLOAD_FIELD = "payload";
	public static final String STREAM_INIT_FIELD = "_init";
	public static final String MESSAGE_PLACEHOLDER_REGEX = "\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}";
	public static final String EVENT_NO_REGEX = "^[a-z][a-z0-9_]*\\.(warning|system|normal)\\.[a-z0-9_]+(\\.[a-z0-9_]+)*:\\d{16}$";
}
