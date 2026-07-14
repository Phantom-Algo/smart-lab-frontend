package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper.UserAccountMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAccountRepositoryImpl implements UserAccountRepository {

	private final UserAccountMapper userAccountMapper;

	@Override
	public Optional<UserAccountDO> findById(Long id) {
		return Optional.ofNullable(userAccountMapper.selectById(id));
	}

	@Override
	public Optional<UserAccountDO> findByUsername(String username) {
		return Optional.ofNullable(userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountDO>()
				.eq(UserAccountDO::getUsername, username)
				.isNull(UserAccountDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public Optional<UserAccountDO> findByPersonId(Long personId) {
		return Optional.ofNullable(userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountDO>()
				.eq(UserAccountDO::getPersonId, personId)
				.isNull(UserAccountDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public List<UserAccountDO> listByPersonIds(List<Long> personIds) {
		if (personIds == null || personIds.isEmpty()) {
			return List.of();
		}
		return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
				.in(UserAccountDO::getPersonId, personIds)
				.isNull(UserAccountDO::getDeletedAt));
	}

	@Override
	public List<Long> listActiveUserIds() {
		return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
						.select(UserAccountDO::getId)
						.eq(UserAccountDO::getAccountStatus, AccountStatus.NORMAL.getCode())
						.isNull(UserAccountDO::getDeletedAt))
				.stream()
				.map(UserAccountDO::getId)
				.toList();
	}

	@Override
	public List<Long> listActiveUserIdsByUserType(Integer userType) {
		return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
						.select(UserAccountDO::getId)
						.eq(UserAccountDO::getAccountStatus, AccountStatus.NORMAL.getCode())
						.eq(UserAccountDO::getUserType, userType)
						.isNull(UserAccountDO::getDeletedAt))
				.stream()
				.map(UserAccountDO::getId)
				.toList();
	}

	@Override
	public void insert(UserAccountDO userAccountDO) {
		userAccountMapper.insert(userAccountDO);
	}

	@Override
	public void update(UserAccountDO userAccountDO) {
		userAccountMapper.updateById(userAccountDO);
	}

	@Override
	public void updateLoginSuccess(Long userId, String lastLoginIp, LocalDateTime lastLoginAt) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getId, userId)
				.set(UserAccountDO::getLastLoginIp, lastLoginIp)
				.set(UserAccountDO::getLastLoginAt, lastLoginAt)
				.set(UserAccountDO::getFailedLoginCount, 0)
				.set(UserAccountDO::getLockExpiresAt, null)
				.set(UserAccountDO::getAccountStatus, AccountStatus.NORMAL.getCode()));
	}

	@Override
	public void increaseFailedLoginCount(Long userId) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getId, userId)
				.setSql("failed_login_count = failed_login_count + 1"));
	}

	@Override
	public void resetFailedLoginState(Long userId) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getId, userId)
				.set(UserAccountDO::getFailedLoginCount, 0)
				.set(UserAccountDO::getLockExpiresAt, null)
				.set(UserAccountDO::getAccountStatus, AccountStatus.NORMAL.getCode()));
	}

	@Override
	public void lockAccount(Long userId, LocalDateTime lockExpiresAt) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getId, userId)
				.set(UserAccountDO::getAccountStatus, AccountStatus.LOCKED.getCode())
				.set(UserAccountDO::getLockExpiresAt, lockExpiresAt));
	}

	@Override
	public void unlockAccount(Long userId) {
		resetFailedLoginState(userId);
	}

	@Override
	public void updatePassword(Long userId, String passwordHash, String passwordSalt, LocalDateTime changedAt) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getId, userId)
				.set(UserAccountDO::getPasswordHash, passwordHash)
				.set(UserAccountDO::getPasswordSalt, passwordSalt)
				.set(UserAccountDO::getLastPasswordChangedAt, changedAt)
				.set(UserAccountDO::getIsPasswordResetRequired, 0));
	}

	@Override
	public void updateAccountStatusByPersonId(Long personId, Integer accountStatus) {
		userAccountMapper.update(new LambdaUpdateWrapper<UserAccountDO>()
				.eq(UserAccountDO::getPersonId, personId)
				.set(UserAccountDO::getAccountStatus, accountStatus));
	}
}
