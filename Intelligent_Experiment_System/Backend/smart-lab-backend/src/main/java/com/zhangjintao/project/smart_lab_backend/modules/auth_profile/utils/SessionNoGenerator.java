package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class SessionNoGenerator {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private SessionNoGenerator() {
	}

	public static String generate() {
		return "SESS" + LocalDateTime.now().format(FORMATTER)
				+ UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}
}
