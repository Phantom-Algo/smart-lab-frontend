package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;

import java.util.Optional;

public interface UserProfileRepository {

	Optional<UserProfileDO> findByUserId(Long userId);

	boolean existsByPhone(String phone, Long excludeUserId);

	boolean existsByEmail(String email, Long excludeUserId);

	void insert(UserProfileDO userProfileDO);

	void update(UserProfileDO userProfileDO);
}
