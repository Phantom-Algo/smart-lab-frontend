package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.MsgDeadLetterMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgDeadLetterRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageDeadLetter;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter.MessageInboxConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 死信消息仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MsgDeadLetterRepositoryImpl implements MsgDeadLetterRepository {

	private final MsgDeadLetterMapper msgDeadLetterMapper;

	@Override
	public void insert(MessageDeadLetter deadLetter) {
		msgDeadLetterMapper.insert(MessageInboxConverter.toMsgDeadLetterDO(deadLetter));
	}
}
