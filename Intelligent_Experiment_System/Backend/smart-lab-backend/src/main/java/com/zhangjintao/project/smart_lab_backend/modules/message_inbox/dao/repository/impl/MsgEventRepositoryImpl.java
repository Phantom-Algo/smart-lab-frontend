package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgEventDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.MsgEventMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgEventRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.value.MessageContentSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DispatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter.MessageInboxConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 消息事件仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MsgEventRepositoryImpl implements MsgEventRepository {

	private final ObjectMapper objectMapper;
	private final MsgEventMapper msgEventMapper;

	@Override
	public Optional<MessageEvent> findByEventNo(String eventNo) {
		return Optional.ofNullable(msgEventMapper.selectOne(new LambdaQueryWrapper<MsgEventDO>()
						.eq(MsgEventDO::getEventNo, eventNo)))
				.map(eventDO -> MessageInboxConverter.toMessageEvent(eventDO, objectMapper));
	}

	@Override
	public MessageEvent insert(MessageEvent messageEvent) {
		MsgEventDO eventDO = MessageInboxConverter.toMsgEventDO(messageEvent, objectMapper);
		msgEventMapper.insert(eventDO);
		return MessageInboxConverter.toMessageEvent(eventDO, objectMapper);
	}

	@Override
	public void markProcessing(Long id) {
		msgEventMapper.update(new LambdaUpdateWrapper<MsgEventDO>()
				.eq(MsgEventDO::getId, id)
				.set(MsgEventDO::getDispatchStatus, DispatchStatus.PROCESSING.getCode()));
	}

	@Override
	public void markCompleted(Long id, MessageContentSnapshot contentSnapshot) {
		msgEventMapper.update(new LambdaUpdateWrapper<MsgEventDO>()
				.eq(MsgEventDO::getId, id)
				.set(MsgEventDO::getTitle, contentSnapshot.getTitle())
				.set(MsgEventDO::getContent, contentSnapshot.getContent())
				.set(MsgEventDO::getContentFormat, contentSnapshot.getContentFormat().getCode())
				.set(MsgEventDO::getDispatchStatus, DispatchStatus.COMPLETED.getCode())
				.set(MsgEventDO::getProcessedAt, LocalDateTime.now()));
	}

	@Override
	public void markFailed(Long id) {
		msgEventMapper.update(new LambdaUpdateWrapper<MsgEventDO>()
				.eq(MsgEventDO::getId, id)
				.set(MsgEventDO::getDispatchStatus, DispatchStatus.FAILED.getCode())
				.setSql("retry_count = retry_count + 1"));
	}
}
