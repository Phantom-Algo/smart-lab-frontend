package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;

import java.util.List;
import java.util.Optional;

public interface LoginSessionRepository {

	Optional<LoginSessionDO> findActiveByAccessTokenHash(String accessTokenHash);

	Optional<LoginSessionDO> findById(Long id);

	List<LoginSessionDO> listRecentByUserId(Long userId, int limit);

	void insert(LoginSessionDO loginSessionDO);

	void markLoggedOut(Long sessionId);

	void invalidateAllByUserId(Long userId, Long excludeSessionId);

	void markExpiredSessions();
}
