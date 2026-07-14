package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils;

import cn.hutool.crypto.digest.DigestUtil;

public final class TokenHashUtils {

	private TokenHashUtils() {
	}

	public static String sha256Hex(String value) {
		return DigestUtil.sha256Hex(value);
	}
}
