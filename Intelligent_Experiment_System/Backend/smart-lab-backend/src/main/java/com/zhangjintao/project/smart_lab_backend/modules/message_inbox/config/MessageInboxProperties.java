package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "message.inbox")
public class MessageInboxProperties {

	private Dispatcher dispatcher = new Dispatcher();

	@Data
	public static class Dispatcher {

		private String streamKey = "smart_lab:message_inbox:stream";

		private String consumerGroup = "message-inbox-dispatcher";

		private String consumerName = "smart-lab-backend";

		private long fixedDelayMs = 5000;

		private int batchSize = 20;

		private int maxRetryCount = 3;

		private long claimIdleMs = 60000;

		private Executor executor = new Executor();
	}

	@Data
	public static class Executor {

		private int corePoolSize = 1;

		private int maxPoolSize = 1;

		private int queueCapacity = 10;
	}
}
