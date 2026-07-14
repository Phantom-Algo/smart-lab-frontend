package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgDeadLetterRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgEventRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageDeadLetter;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DeadLetterStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 死信服务实现
 */
@Service
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {

	private final ObjectMapper objectMapper;
	private final MsgEventRepository msgEventRepository;
	private final MsgDeadLetterRepository msgDeadLetterRepository;

	@Override
	public void save(MessagePublishCommand command, FailureStage failureStage, Throwable throwable, boolean retryable) {
		Long eventId = Optional.ofNullable(command)
				.map(MessagePublishCommand::getEventNo)
				.flatMap(msgEventRepository::findByEventNo)
				.map(MessageEvent::getId)
				.orElse(null);
		MessageDeadLetter deadLetter = MessageDeadLetter.builder()
				.eventId(eventId)
				.eventNo(command == null ? null : command.getEventNo())
				.failureStage(failureStage)
				.errorCode(resolveErrorCode(throwable))
				.errorMessage(resolveErrorMessage(throwable))
				.rawPayload(serialize(command))
				.retryable(retryable)
				.status(DeadLetterStatus.PENDING)
				.build();
		msgDeadLetterRepository.insert(deadLetter);
	}

	@Override
	public void saveRawPayload(String rawPayload, FailureStage failureStage, Throwable throwable) {
		MessageDeadLetter deadLetter = MessageDeadLetter.builder()
				.failureStage(failureStage)
				.errorCode(resolveErrorCode(throwable))
				.errorMessage(resolveErrorMessage(throwable))
				.rawPayload(wrapRawPayload(rawPayload))
				.retryable(false)
				.status(DeadLetterStatus.PENDING)
				.build();
		msgDeadLetterRepository.insert(deadLetter);
	}

	private String resolveErrorCode(Throwable throwable) {
		if (throwable instanceof BusinessException businessException) {
			return String.valueOf(businessException.getCode());
		}
		return throwable.getClass().getSimpleName();
	}

	private String resolveErrorMessage(Throwable throwable) {
		return throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage();
	}

	private String serialize(MessagePublishCommand command) {
		if (command == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(command);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("死信原始命令序列化失败", ex);
		}
	}

	private String wrapRawPayload(String rawPayload) {
		try {
			return objectMapper.writeValueAsString(new RawPayload(rawPayload));
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("死信原始 payload 序列化失败", ex);
		}
	}

	private record RawPayload(String rawPayload) {
	}
}
