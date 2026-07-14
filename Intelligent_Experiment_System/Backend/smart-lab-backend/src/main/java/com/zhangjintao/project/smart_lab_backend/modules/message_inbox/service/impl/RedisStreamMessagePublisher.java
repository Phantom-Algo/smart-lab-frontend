package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MessageRedisStreamRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageDispatchProcessor;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublishValidator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamMessagePublisher implements MessagePublisher {

	private final ObjectMapper objectMapper;
	private final MessagePublishValidator messagePublishValidator;
	private final MessageRedisStreamRepository messageRedisStreamRepository;
	private final MessageDispatchProcessor messageDispatchProcessor;

	@Override
	public MessagePublishResult publish(MessagePublishCommand command) {
		// 1. 校验消息发布命令
		messagePublishValidator.validate(command);

		// 2. 序列化消息发布命令并写入 Redis Stream
		String payload = serialize(command);
		String streamRecordId = null;
		try {
			streamRecordId = messageRedisStreamRepository.addPayload(payload);
		}
		catch (Exception ex) {
			log.warn("Message stream append failed, will try direct dispatch, eventNo={}", command.getEventNo(), ex);
		}

		// 3. 本地演示环境里 Redis/调度器可能未启动，立即分发一次保证收件箱可见。
		try {
			messageDispatchProcessor.process(command);
		}
		catch (Exception ex) {
			if (streamRecordId != null) {
				log.warn("Direct message dispatch failed, waiting for dispatcher retry, eventNo={}",
						command.getEventNo(), ex);
			}
			else {
				throw new MessageInboxException(MessageInboxErrorCode.MESSAGE_DISPATCH_FAILED, "消息直接分发失败");
			}
		}

		// 4. 返回消息发布结果
		return MessagePublishResult.builder()
				.eventNo(command.getEventNo())
				.streamRecordId(streamRecordId)
				.build();
	}

	private String serialize(MessagePublishCommand command) {
		try {
			return objectMapper.writeValueAsString(command);
		}
		catch (JsonProcessingException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.INVALID_MESSAGE_EVENT, "消息发布命令序列化失败");
		}
	}
}
