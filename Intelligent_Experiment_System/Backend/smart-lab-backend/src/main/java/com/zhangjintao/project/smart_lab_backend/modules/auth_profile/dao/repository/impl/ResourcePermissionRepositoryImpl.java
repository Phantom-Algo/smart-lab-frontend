package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.ResourcePermissionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.mapper.ResourcePermissionMapper;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.ResourcePermissionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.PermissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ResourcePermissionRepositoryImpl implements ResourcePermissionRepository {

	private final ResourcePermissionMapper resourcePermissionMapper;

	@Override
	public List<ResourcePermissionDO> listActiveByUserId(Long userId) {
		return resourcePermissionMapper.selectList(buildBaseQuery(userId)
				.orderByDesc(ResourcePermissionDO::getCreatedAt));
	}

	@Override
	public List<ResourcePermissionDO> listActiveByUserIdAndResourceType(Long userId, Integer resourceType) {
		return resourcePermissionMapper.selectList(buildBaseQuery(userId)
				.eq(ResourcePermissionDO::getResourceType, resourceType)
				.orderByDesc(ResourcePermissionDO::getCreatedAt));
	}

	@Override
	public boolean existsActivePermission(Long userId, Integer resourceType, Long resourceId, String actionCode) {
		return resourcePermissionMapper.selectCount(buildBaseQuery(userId)
				.eq(ResourcePermissionDO::getResourceType, resourceType)
				.eq(ResourcePermissionDO::getResourceId, resourceId)
				.eq(ResourcePermissionDO::getActionCode, actionCode)) > 0;
	}

	private LambdaQueryWrapper<ResourcePermissionDO> buildBaseQuery(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		return new LambdaQueryWrapper<ResourcePermissionDO>()
				.eq(ResourcePermissionDO::getUserId, userId)
				.eq(ResourcePermissionDO::getPermissionStatus, PermissionStatus.ACTIVE.getCode())
				.and(wrapper -> wrapper.isNull(ResourcePermissionDO::getEffectiveAt)
						.or()
						.le(ResourcePermissionDO::getEffectiveAt, now))
				.and(wrapper -> wrapper.isNull(ResourcePermissionDO::getExpireAt)
						.or()
						.gt(ResourcePermissionDO::getExpireAt, now))
				.isNull(ResourcePermissionDO::getDeletedAt);
	}
}
