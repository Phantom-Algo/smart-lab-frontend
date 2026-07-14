package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper.UserProfileMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {

	private final UserProfileMapper userProfileMapper;

	@Override
	public Optional<UserProfileDO> findByUserId(Long userId) {
		return Optional.ofNullable(userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfileDO>()
				.eq(UserProfileDO::getUserId, userId)
				.isNull(UserProfileDO::getDeletedAt)
				.last("limit 1")));
	}

	@Override
	public boolean existsByPhone(String phone, Long excludeUserId) {
		if (!StringUtils.hasText(phone)) {
			return false;
		}
		LambdaQueryWrapper<UserProfileDO> queryWrapper = new LambdaQueryWrapper<UserProfileDO>()
				.eq(UserProfileDO::getPhone, phone)
				.isNull(UserProfileDO::getDeletedAt);
		if (excludeUserId != null) {
			queryWrapper.ne(UserProfileDO::getUserId, excludeUserId);
		}
		return userProfileMapper.selectCount(queryWrapper) > 0;
	}

	@Override
	public boolean existsByEmail(String email, Long excludeUserId) {
		if (!StringUtils.hasText(email)) {
			return false;
		}
		LambdaQueryWrapper<UserProfileDO> queryWrapper = new LambdaQueryWrapper<UserProfileDO>()
				.eq(UserProfileDO::getEmail, email)
				.isNull(UserProfileDO::getDeletedAt);
		if (excludeUserId != null) {
			queryWrapper.ne(UserProfileDO::getUserId, excludeUserId);
		}
		return userProfileMapper.selectCount(queryWrapper) > 0;
	}

	@Override
	public void insert(UserProfileDO userProfileDO) {
		userProfileMapper.insert(userProfileDO);
	}

	@Override
	public void update(UserProfileDO userProfileDO) {
		userProfileMapper.updateById(userProfileDO);
	}
}
