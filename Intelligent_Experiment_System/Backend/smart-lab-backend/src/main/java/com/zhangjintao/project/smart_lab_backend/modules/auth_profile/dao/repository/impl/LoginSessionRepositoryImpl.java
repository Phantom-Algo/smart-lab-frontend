package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper.LoginSessionMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.LoginSessionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoginSessionRepositoryImpl implements LoginSessionRepository {

	private final LoginSessionMapper loginSessionMapper;

	@Override
	public Optional<LoginSessionDO> findActiveByAccessTokenHash(String accessTokenHash) {
		return Optional.ofNullable(loginSessionMapper.selectOne(new LambdaQueryWrapper<LoginSessionDO>()
				.eq(LoginSessionDO::getAccessTokenHash, accessTokenHash)
				.eq(LoginSessionDO::getSessionStatus, SessionStatus.ONLINE.getCode())
				.gt(LoginSessionDO::getExpireAt, LocalDateTime.now())
				.isNull(LoginSessionDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public Optional<LoginSessionDO> findById(Long id) {
		return Optional.ofNullable(loginSessionMapper.selectById(id));
	}

	@Override
	public List<LoginSessionDO> listRecentByUserId(Long userId, int limit) {
		return loginSessionMapper.selectList(new LambdaQueryWrapper<LoginSessionDO>()
				.eq(LoginSessionDO::getUserId, userId)
				.isNull(LoginSessionDO::getDeletedAt)
				.orderByDesc(LoginSessionDO::getLoginAt)
				.last("limit " + limit));
	}

	@Override
	public void insert(LoginSessionDO loginSessionDO) {
		loginSessionMapper.insert(loginSessionDO);
	}

	@Override
	public void markLoggedOut(Long sessionId) {
		loginSessionMapper.update(new LambdaUpdateWrapper<LoginSessionDO>()
				.eq(LoginSessionDO::getId, sessionId)
				.eq(LoginSessionDO::getSessionStatus, SessionStatus.ONLINE.getCode())
				.set(LoginSessionDO::getSessionStatus, SessionStatus.LOGGED_OUT.getCode())
				.set(LoginSessionDO::getLogoutAt, LocalDateTime.now()));
	}

	@Override
	public void invalidateAllByUserId(Long userId, Long excludeSessionId) {
		LambdaUpdateWrapper<LoginSessionDO> updateWrapper = new LambdaUpdateWrapper<LoginSessionDO>()
				.eq(LoginSessionDO::getUserId, userId)
				.eq(LoginSessionDO::getSessionStatus, SessionStatus.ONLINE.getCode())
				.set(LoginSessionDO::getSessionStatus, SessionStatus.INVALIDATED.getCode())
				.set(LoginSessionDO::getLogoutAt, LocalDateTime.now());
		if (excludeSessionId != null) {
			updateWrapper.ne(LoginSessionDO::getId, excludeSessionId);
		}
		loginSessionMapper.update(updateWrapper);
	}

	@Override
	public void markExpiredSessions() {
		loginSessionMapper.update(new LambdaUpdateWrapper<LoginSessionDO>()
				.eq(LoginSessionDO::getSessionStatus, SessionStatus.ONLINE.getCode())
				.le(LoginSessionDO::getExpireAt, LocalDateTime.now())
				.set(LoginSessionDO::getSessionStatus, SessionStatus.EXPIRED.getCode()));
	}
}
