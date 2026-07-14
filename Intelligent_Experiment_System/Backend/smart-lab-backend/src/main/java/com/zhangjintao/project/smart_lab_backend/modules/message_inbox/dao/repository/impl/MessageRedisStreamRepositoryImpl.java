package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config.MessageInboxProperties;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MessageRedisStreamRepository;
import io.lettuce.core.RedisBusyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 消息仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRedisStreamRepositoryImpl implements MessageRedisStreamRepository {

	private final StringRedisTemplate stringRedisTemplate;
	private final MessageInboxProperties messageInboxProperties;

	@Override
	public String addPayload(String payload) {
		RecordId recordId = streamOperations().add(streamKey(), Map.of(MessageInboxConstants.STREAM_PAYLOAD_FIELD, payload),
				RedisStreamCommands.XAddOptions.maxlen(100_000).approximateTrimming(true));
		if (recordId == null) {
			throw new IllegalStateException("Redis Stream 写入失败");
		}
		return recordId.getValue();
	}

	/**
	 * 认领超时未确认的待处理消息
	 */
	@Override
	public List<StreamMessageRecord> claimIdleMessages() {
		// 1. 获取待处理的消息列表（Pending Messages）
		PendingMessages pendingMessages = streamOperations().pending(
			streamKey(), // Stream key
			dispatcherProperties().getConsumerGroup(), // Consumer group name
			Range.unbounded(), // 范围-不限制
			dispatcherProperties().getBatchSize() // 批量大小
		);
		if (pendingMessages.isEmpty()) {
			return List.of();
		}

		// 2. 构建消息ID到投递次数的映射，用于后续认领消息时使用
		Map<String, Long> deliveryCountMap = new HashMap<>();
		RecordId[] recordIds = pendingMessages.stream()
				.peek(message -> deliveryCountMap.put(message.getIdAsString(), message.getTotalDeliveryCount()))
				.map(PendingMessage::getId)
				.toArray(RecordId[]::new);

		// 3. 认领超时未确认的待处理消息
		List<MapRecord<String, String, String>> claimedRecords = streamOperations().claim(streamKey(),
				dispatcherProperties().getConsumerGroup(),
				dispatcherProperties().getConsumerName(),
				Duration.ofMillis(dispatcherProperties().getClaimIdleMs()),
				recordIds);

		// 4. 将认领到的消息封装为 StreamMessageRecord 对象并返回
		return claimedRecords.stream()
				.map(record -> new StreamMessageRecord(record, deliveryCountMap.getOrDefault(record.getId().getValue(), 1L)))
				.toList();
	}

	/**
	 * 读取新消息
	 */
	@Override
	public List<StreamMessageRecord> readNewMessages() {
		// 向 Redis 发起 XREADGROUP 命令，读取新消息
		@SuppressWarnings("unchecked")
		List<MapRecord<String, String, String>> records = streamOperations().read(
			consumer(), // 消费者身份
			StreamReadOptions.empty().count(dispatcherProperties().getBatchSize()), // 不阻塞读，且每次最多读取批量大小的消息
			StreamOffset.create(streamKey(), ReadOffset.lastConsumed()) // 从上次已消费的位置开始读取新消息
		);
		if (records == null || records.isEmpty()) {
			return List.of();
		}
		return records.stream().map(record -> new StreamMessageRecord(record, 1L)).toList();
	}

	@Override
	public void ack(String recordId) {
		streamOperations().acknowledge(streamKey(), dispatcherProperties().getConsumerGroup(), recordId);
	}

	/**
	 * 确保消费组已创建
	 * 由于 Redis 规定 XGROUP CREATE 必须在 Stream 存在时才能创建消费组，因此需要先判断 Stream 是否存在。
	 */
	@Override
	public void ensureConsumerGroup() {
		// 1. 分支1: Stream 不存在时
		if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey()))) {
			// Stream 不存在，先创建一个临时消息以便创建消费组
			RecordId initRecordId = streamOperations().add(streamKey(), Map.of(MessageInboxConstants.STREAM_INIT_FIELD, "true"));
			// 创建消费组
			createConsumerGroup();
			// 删除临时消息，清理干净的 Stream
			if (initRecordId != null) {
				streamOperations().delete(streamKey(), initRecordId);
			}
			return;
		}

		// 分支2: Stream 已存在，直接创建消费组
		createConsumerGroup();
	}

	private void createConsumerGroup() {
		try {
			streamOperations().createGroup(
				streamKey(), 
				ReadOffset.from("0-0"), 
				dispatcherProperties().getConsumerGroup()
			);
		}
		catch (RuntimeException ex) {
			// 如果捕获到的是 BUSYGROUP 异常，说明消费组已存在，直接返回即可
			if (isBusyGroupException(ex)) {
				return;
			}
			// 如果不是 BUSYGROUP 异常，则继续抛出异常
			throw ex;
		}
	}

	// 递归查找 Redis BUSYGROUP 异常 - 因为 Spring Data Redis 可能将 Redis 原生错误 BUSYGROUP 包装多层，所以要沿着异常链递归查找
	private boolean isBusyGroupException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof RedisBusyException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null && message.contains("BUSYGROUP")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private Consumer consumer() {
		return Consumer.from(dispatcherProperties().getConsumerGroup(), dispatcherProperties().getConsumerName());
	}

	private MessageInboxProperties.Dispatcher dispatcherProperties() {
		return messageInboxProperties.getDispatcher();
	}

	private String streamKey() {
		return dispatcherProperties().getStreamKey();
	}

	private StreamOperations<String, String, String> streamOperations() {
		return stringRedisTemplate.opsForStream();
	}
}
