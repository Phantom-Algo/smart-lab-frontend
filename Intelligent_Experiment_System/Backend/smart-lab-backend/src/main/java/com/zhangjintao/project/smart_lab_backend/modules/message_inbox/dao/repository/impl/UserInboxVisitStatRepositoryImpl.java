package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.UserInboxVisitStatMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxVisitStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户收件箱查看状态仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class UserInboxVisitStatRepositoryImpl implements UserInboxVisitStatRepository {

	private final UserInboxVisitStatMapper userInboxVisitStatMapper;

	@Override
	public Optional<LocalDateTime> findLastViewedAt(Long userId) {
		return Optional.ofNullable(userInboxVisitStatMapper.selectLastViewedAt(userId));
	}

	@Override
	public int upsertLastViewedAt(Long userId, LocalDateTime lastViewedAt) {
		return userInboxVisitStatMapper.upsertLastViewedAt(userId, lastViewedAt);
	}
}
