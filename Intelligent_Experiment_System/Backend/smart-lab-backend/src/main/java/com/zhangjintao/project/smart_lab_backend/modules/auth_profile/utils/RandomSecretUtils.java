package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class RandomSecretUtils {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private RandomSecretUtils() {
	}

	public static String randomToken(int byteLength) {
		byte[] buffer = new byte[byteLength];
		SECURE_RANDOM.nextBytes(buffer);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
	}
}
