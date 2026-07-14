package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.ResourcePermissionDO;

import java.util.List;

public interface ResourcePermissionRepository {

	List<ResourcePermissionDO> listActiveByUserId(Long userId);

	List<ResourcePermissionDO> listActiveByUserIdAndResourceType(Long userId, Integer resourceType);

	boolean existsActivePermission(Long userId, Integer resourceType, Long resourceId, String actionCode);
}
