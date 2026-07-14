package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.config.MessageInboxProperties;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MessageRedisStreamRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.DeadLetterService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageDispatchProcessor;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageDispatchWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息分发工作器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDispatchWorkerImpl implements MessageDispatchWorker {

	private final ObjectMapper objectMapper;
	private final MessageInboxProperties messageInboxProperties;
	private final DeadLetterService deadLetterService;
	private final MessageDispatchProcessor messageDispatchProcessor;
	private final MessageRedisStreamRepository messageRedisStreamRepository;

	@Override
	public void dispatchOnce() {
		messageRedisStreamRepository.ensureConsumerGroup();
		List<MessageRedisStreamRepository.StreamMessageRecord> records = new ArrayList<>();
		records.addAll(messageRedisStreamRepository.claimIdleMessages());
		records.addAll(messageRedisStreamRepository.readNewMessages());
		for (MessageRedisStreamRepository.StreamMessageRecord record : records) {
			processOne(record);
		}
	}

	private void processOne(MessageRedisStreamRepository.StreamMessageRecord streamMessageRecord) {
		MapRecord<String, String, String> record = streamMessageRecord.getRecord();
		String recordId = record.getId().getValue();
		String payload = extractPayload(record);
		if (payload == null) {
			messageRedisStreamRepository.ack(recordId);
			return;
		}
		MessagePublishCommand command = null;
		try {
			command = objectMapper.readValue(payload, MessagePublishCommand.class);

			// messageDispatchProcessor.process(command) 真正执行消息分发逻辑
			messageDispatchProcessor.process(command);
			messageRedisStreamRepository.ack(recordId);
		}
		catch (Exception ex) {
			handleFailure(recordId, streamMessageRecord.getDeliveryCount(), payload, command, ex);
		}
	}

	private void handleFailure(String recordId, long deliveryCount, String payload, MessagePublishCommand command, Exception ex) {
		log.warn("Message dispatch failed, recordId={}, deliveryCount={}", recordId, deliveryCount, ex);
		if (command != null) {
			messageDispatchProcessor.markFailed(command.getEventNo());
		}
		if (deliveryCount >= messageInboxProperties.getDispatcher().getMaxRetryCount()) {
			if (command == null) {
				deadLetterService.saveRawPayload(payload, FailureStage.VALIDATE, ex);
			}
			else {
				deadLetterService.save(command, FailureStage.DISPATCH, ex, false);
			}
			messageRedisStreamRepository.ack(recordId);
		}
	}
	
	private String extractPayload(MapRecord<String, String, String> record) {
		Map<String, String> value = record.getValue();
		String payload = value.get(MessageInboxConstants.STREAM_PAYLOAD_FIELD);
		if (payload == null) {
			return null;
		}
		return payload;
	}
}
