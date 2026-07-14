package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config.MessageInboxProperties;
import io.lettuce.core.RedisBusyException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageRedisStreamRepositoryImplTest {

	@Test
	@SuppressWarnings("unchecked")
	void ensureConsumerGroupIgnoresBusyGroupException() {
		StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
		StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
		MessageInboxProperties properties = new MessageInboxProperties();
		MessageRedisStreamRepositoryImpl repository = new MessageRedisStreamRepositoryImpl(stringRedisTemplate, properties);
		when(stringRedisTemplate.hasKey(properties.getDispatcher().getStreamKey())).thenReturn(true);
		when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
		when(streamOperations.createGroup(properties.getDispatcher().getStreamKey(), org.springframework.data.redis.connection.stream.ReadOffset.from("0-0"),
				properties.getDispatcher().getConsumerGroup()))
				.thenThrow(new RuntimeException("group exists", new RedisBusyException("BUSYGROUP Consumer Group name already exists")));

		assertThatCode(repository::ensureConsumerGroup).doesNotThrowAnyException();

		verify(streamOperations).createGroup(properties.getDispatcher().getStreamKey(),
				org.springframework.data.redis.connection.stream.ReadOffset.from("0-0"),
				properties.getDispatcher().getConsumerGroup());
	}
}