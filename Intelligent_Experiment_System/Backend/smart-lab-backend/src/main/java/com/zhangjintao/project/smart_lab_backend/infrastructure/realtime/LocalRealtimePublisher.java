package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEnvelope;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeTargetType;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket.RealtimeSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalRealtimePublisher implements RealtimePublisher {

	private final ObjectMapper objectMapper;
	private final RealtimeSessionRegistry sessionRegistry;

	@Override
	public void publish(RealtimeEvent event) {
		if (event == null) {
			return;
		}
		RealtimeEnvelope envelope = RealtimeEnvelope.builder()
				.event(event.getEvent())
				.source(event.getSource())
				.occurredAt(event.getOccurredAt() == null ? LocalDateTime.now() : event.getOccurredAt())
				.data(event.getData())
				.build();
		String payload = writeEnvelope(envelope);
		if (event.getTargetType() == RealtimeTargetType.USERS) {
			sessionRegistry.sendToUsers(event.getTargetUserIds(), payload);
			return;
		}
		sessionRegistry.broadcast(payload);
	}

	@Override
	public void publishAfterCommit(RealtimeEvent event) {
		Runnable task = () -> publish(event);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			task.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}

	private String writeEnvelope(RealtimeEnvelope envelope) {
		try {
			return objectMapper.writeValueAsString(envelope);
		}
		catch (JsonProcessingException ex) {
			log.error("实时事件序列化失败, event={}", envelope.getEvent(), ex);
			throw new IllegalStateException("实时事件序列化失败", ex);
		}
	}
}