package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class TeachingNoGenerator {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private TeachingNoGenerator() {
	}

	public static String generateTaskNo() {
		return "TT" + FORMATTER.format(LocalDateTime.now()) + randomSuffix();
	}

	public static String generateResourceNo() {
		return "TR" + FORMATTER.format(LocalDateTime.now()) + randomSuffix();
	}

	private static String randomSuffix() {
		return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
	}
}
