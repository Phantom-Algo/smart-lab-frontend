package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MessageInboxProperties.class)
public class MessageInboxConfig {

	private final MessageInboxProperties messageInboxProperties;

	@Bean("messageInboxDispatcherExecutor")
	public Executor messageInboxDispatcherExecutor() {
		MessageInboxProperties.Executor executorProperties = messageInboxProperties.getDispatcher().getExecutor();
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(executorProperties.getCorePoolSize());
		executor.setMaxPoolSize(executorProperties.getMaxPoolSize());
		executor.setQueueCapacity(executorProperties.getQueueCapacity());
		executor.setThreadNamePrefix("message-inbox-dispatcher-");
		executor.initialize();
		return executor;
	}
}
