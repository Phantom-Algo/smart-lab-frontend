package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.utils;

import cn.hutool.core.util.RandomUtil;

public final class MessageEventNoGenerator {

	private MessageEventNoGenerator() {
	}

	public static String generate(String messageType) {
		MessageTypeUtils.validateMessageType(messageType);
		return messageType + ":" + RandomUtil.randomNumbers(16);
	}
}
