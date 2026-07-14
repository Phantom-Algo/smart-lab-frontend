package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-lab.auth")
public class AuthProfileProperties {

	private Session session = new Session();

	private Password password = new Password();

	@Data
	public static class Session {

		private long accessTokenTtlSeconds = 7200;

		private int recentSessionLimit = 10;
	}

	@Data
	public static class Password {

		private int maxFailedAttempts = 5;

		private long lockDurationMinutes = 30;

		private int minLength = 8;
	}
}
