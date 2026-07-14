package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

	Optional<UserAccountDO> findById(Long id);

	Optional<UserAccountDO> findByUsername(String username);

	Optional<UserAccountDO> findByPersonId(Long personId);

	List<UserAccountDO> listByPersonIds(List<Long> personIds);

	List<Long> listActiveUserIds();

	List<Long> listActiveUserIdsByUserType(Integer userType);

	void insert(UserAccountDO userAccountDO);

	void update(UserAccountDO userAccountDO);

	void updateLoginSuccess(Long userId, String lastLoginIp, LocalDateTime lastLoginAt);

	void increaseFailedLoginCount(Long userId);

	void resetFailedLoginState(Long userId);

	void lockAccount(Long userId, LocalDateTime lockExpiresAt);

	void unlockAccount(Long userId);

	void updatePassword(Long userId, String passwordHash, String passwordSalt, LocalDateTime changedAt);

	void updateAccountStatusByPersonId(Long personId, Integer accountStatus);
}
