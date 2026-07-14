package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.util.List;

/**
 * Redis Stream 消息仓储接口
 */
public interface MessageRedisStreamRepository {

	/**
	 * 向 Stream 中添加消息负载
	 *
	 * @param payload 消息负载
	 * @return Stream 记录ID
	 */
	String addPayload(String payload);

	/**
	 * 认领超时未确认的待处理消息
	 *
	 * @return 消息记录列表
	 */
	List<StreamMessageRecord> claimIdleMessages();

	/**
	 * 读取新消息
	 *
	 * @return 消息记录列表
	 */
	List<StreamMessageRecord> readNewMessages();

	/**
	 * 确认消息处理完成
	 *
	 * @param recordId 记录ID
	 */
	void ack(String recordId);

	/**
	 * 确保消费者组已创建
	 */
	void ensureConsumerGroup();

	/**
	 * Stream 消息记录
	 */
	@Getter
	@RequiredArgsConstructor
	class StreamMessageRecord {

		/** 原始记录 */
		private final MapRecord<String, String, String> record;

		/** 投递次数 */
		private final long deliveryCount;
	}
}
